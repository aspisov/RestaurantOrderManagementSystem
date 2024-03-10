package service

import db.DatabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDateTime

class OrderService : OrderUpdateSubject {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val observers = mutableListOf<OrderUpdateObserver>()

    override fun attach(observer: OrderUpdateObserver) {
        observers.add(observer)
    }

    override fun detach(observer: OrderUpdateObserver) {
        observers.remove(observer)
    }

    override fun notifyUpdate(orderId: Int, status: String) {
        observers.forEach { observer ->
            observer.onOrderUpdated(orderId, status)
        }
    }

    fun createOrder(userId: Int, dishesWithQuantities: List<Pair<Int, Int>>): Int? {
        var orderId: Int? = null
        DatabaseConfig.getConnection().use { conn ->
            try {
                conn.autoCommit = false

                val orderStmt = conn.prepareStatement(
                    "INSERT INTO orders (user_id, status, created_at) VALUES (?, 'PENDING', ?)",
                    Statement.RETURN_GENERATED_KEYS
                )
                orderStmt.setInt(1, userId)
                orderStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                orderStmt.executeUpdate()

                val generatedKeys = orderStmt.generatedKeys
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1)
                }

                var totalPrepTimeInSeconds = 0
                dishesWithQuantities.forEach { (dishId, quantity) ->
                    val prepTime = getPreparationTimeForDish(dishId) // Реализуйте этот метод
                    totalPrepTimeInSeconds += prepTime * quantity
                }

                dishesWithQuantities.forEach { (dishId, quantity) ->
                    val detailStmt = conn.prepareStatement("INSERT INTO order_details (order_id, dish_id, quantity) VALUES (?, ?, ?)")
                    orderId?.let {
                        detailStmt.setInt(1, it)
                        detailStmt.setInt(2, dishId)
                        detailStmt.setInt(3, quantity)
                        detailStmt.executeUpdate()
                    }
                }

                conn.commit()

                // async order status update after it's done cooking
                orderId?.let { id ->
                    scope.launch {
                        delay(totalPrepTimeInSeconds * 1000L)
                        updateOrderStatusToCooked(id)
                    }
                }

            } catch (e: Exception) {
                conn.rollback()
                e.printStackTrace()
            } finally {
                conn.autoCommit = true
            }
        }
        return orderId
    }

    private suspend fun updateOrderStatusToCooked(orderId: Int) {
        DatabaseConfig.getConnection().use { conn ->
            try {
                conn.autoCommit = false
                val updateStmt = conn.prepareStatement("UPDATE orders SET status = 'COOKED' WHERE id = ?")
                updateStmt.setInt(1, orderId)
                updateStmt.executeUpdate()
                conn.commit()
                notifyUpdate(orderId, "COOKED")
            } catch (e: Exception) {
                conn.rollback()
                e.printStackTrace()
            } finally {
                conn.autoCommit = true
            }
        }
    }

    private fun getPreparationTimeForDish(dishId: Int): Int {
        DatabaseConfig.getConnection().use { conn ->
            val queryStmt = conn.prepareStatement("SELECT preparation_time FROM dishes WHERE id = ?")
            queryStmt.setInt(1, dishId)

            val resultSet = queryStmt.executeQuery()
            if (resultSet.next()) {
                return resultSet.getInt("preparation_time")
            }
        }
        throw IllegalArgumentException("Dish with ID $dishId not found.")
    }


    fun addDishToExistingOrder(orderId: Int, dishId: Int, quantityToAdd: Int) {
        DatabaseConfig.getConnection().use { conn ->
            try {
                conn.autoCommit = false

                // check if order status allows adding a dish
                val statusCheckStmt = conn.prepareStatement("SELECT status FROM orders WHERE id = ?")
                statusCheckStmt.setInt(1, orderId)
                val resultSet = statusCheckStmt.executeQuery()
                if (resultSet.next() && resultSet.getString("status") == "PENDING") {

                    // checking if we already have this dish in order
                    val checkStmt = conn.prepareStatement("""
                    SELECT quantity FROM order_details WHERE order_id = ? AND dish_id = ?
                """.trimIndent())
                    checkStmt.setInt(1, orderId)
                    checkStmt.setInt(2, dishId)
                    val checkRs = checkStmt.executeQuery()

                    if (checkRs.next()) {
                        // if we already have this dish increasing quantity
                        val currentQuantity = checkRs.getInt("quantity")
                        val updateStmt = conn.prepareStatement("""
                        UPDATE order_details SET quantity = ? WHERE order_id = ? AND dish_id = ?
                    """.trimIndent())
                        updateStmt.setInt(1, currentQuantity + quantityToAdd)
                        updateStmt.setInt(2, orderId)
                        updateStmt.setInt(3, dishId)
                        updateStmt.executeUpdate()
                    } else {
                        // if we dont have this dish, we add it into order
                        val insertStmt = conn.prepareStatement("""
                        INSERT INTO order_details (order_id, dish_id, quantity) VALUES (?, ?, ?)
                    """.trimIndent())
                        insertStmt.setInt(1, orderId)
                        insertStmt.setInt(2, dishId)
                        insertStmt.setInt(3, quantityToAdd)
                        insertStmt.executeUpdate()
                    }
                    println("Dish added/updated in the order.")

                    conn.commit()
                } else {
                    println("Cannot add dishes to the order at its current status.")
                }
            } catch (e: Exception) {
                conn.rollback()
                e.printStackTrace()
            } finally {
                conn.autoCommit = true
            }
        }
    }


    fun cancelAllOrdersForUser(userId: Int) {
        DatabaseConfig.getConnection().use { conn ->
            try {
                conn.autoCommit = false

                // making all order's statuses "CANCELLED"
                val stmt = conn.prepareStatement("""
                UPDATE orders 
                SET status = 'CANCELLED' 
                WHERE user_id = ? AND status NOT IN ('CANCELLED', 'COMPLETED')
            """.trimIndent())
                stmt.setInt(1, userId)

                val updatedRows = stmt.executeUpdate()
                println("$updatedRows orders cancelled for user ID: $userId")

                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                e.printStackTrace()
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun viewOrdersForUser(userId: Int) {
        println("Viewing orders for user ID: $userId")

        DatabaseConfig.getConnection().use { conn ->
            val stmt = conn.prepareStatement("""
            SELECT o.id, o.status, d.name, od.quantity
            FROM orders o
            JOIN order_details od ON o.id = od.order_id
            JOIN dishes d ON od.dish_id = d.id
            WHERE o.user_id = ?
            ORDER BY o.id
        """.trimIndent())
            stmt.setInt(1, userId)

            val resultSet = stmt.executeQuery()
            if (!resultSet.next()) {
                println("No orders found for this user.")
                return
            } else {
                println("Orders found for user ID: $userId")
                var currentOrderId = -1 // init with not existing order_id
                do {
                    val orderId = resultSet.getInt("id")
                    if (orderId != currentOrderId) {
                        println("\nOrder ID: $orderId - Status: ${resultSet.getString("status")}")
                        currentOrderId = orderId
                    }
                    val dishName = resultSet.getString("name")
                    val quantity = resultSet.getInt("quantity")
                    println("$quantity x $dishName")
                } while (resultSet.next())
            }
        }
    }

    fun completeAllOrdersForUser(userId: Int) {
        DatabaseConfig.getConnection().use { conn ->
            try {
                conn.autoCommit = false

                // all order's statuses we change to "COMPLETED"
                val updateOrdersStmt = conn.prepareStatement("""
                UPDATE orders 
                SET status = 'COMPLETED' 
                WHERE user_id = ? AND status NOT IN ('COMPLETED', 'CANCELLED')
            """.trimIndent())
                updateOrdersStmt.setInt(1, userId)
                val updatedRows = updateOrdersStmt.executeUpdate()

                conn.commit()

                println("$updatedRows orders marked as completed for user ID: $userId")
            } catch (e: Exception) {
                conn.rollback()
                e.printStackTrace()
            } finally {
                conn.autoCommit = true
            }
        }
    }

}
