package ui

import model.Dish
import model.User
import model.UserRole
import service.*
import java.time.LocalDateTime

fun main() {
    val authService = AuthService()
    val menuService = MenuService()
    val orderService = OrderService()
    val reviewService = ReviewService()
    val statisticsService = StatisticsService()
    val userNotificationService = UserNotificationService()

    // subscribing notification service to order status updates
    orderService.attach(userNotificationService)

    println("Welcome to the Restaurant Management System")

    var currentUser: User? = null

    // user authentication
    auth@ while (true) {
        println("\n1. Login\n2. Register\n3. Exit")
        when (readlnOrNull()) {
            "1" -> {
                println("Username:")
                val username = readln()
                println("Password:")
                val password = readln()
                try {
                    currentUser = authService.authenticateUser(username, password)
                } catch (e: Exception) {
                    println("Error! Try again.")
                    println(e.message)
                }
                if (currentUser == null) println("Login failed")
                else break@auth
            }
            "2" -> {
                println("Registering new user. Choose a username:")
                val username = readln()
                println("Choose a password:")
                val password = readln()
                println("Choose profile type:\n1. Admin\n2. Visitor")
                val type = readln()
                try {
                    when (type) {
                        "1" -> authService.registerUser(username, password, UserRole.ADMIN)
                        else -> authService.registerUser(username, password, UserRole.VISITOR)
                    }
                    println("Registration successful. Please login.")
                } catch (e: Exception) {
                    println("Error! Try again.")
                    println(e.message)
                }
            }
            "3" -> return
            else -> println("Invalid choice, please try again.")
        }
    }

    // depending on user role we load a menu
    if (currentUser != null) {
        when (currentUser.role) {
            UserRole.ADMIN -> adminMenu(menuService, reviewService, statisticsService)
            UserRole.VISITOR -> visitorMenu(menuService, orderService, reviewService, currentUser.id)
        }
    }
}

fun adminMenu(menuService: MenuService, reviewService: ReviewService, statisticsService: StatisticsService) {
    loop@while (true) {
        println("\nAdmin Menu:")
        println("1. Add Dish")
        println("2. Remove Dish")
        println("3. View Most Popular Dishes")
        println("4. View Order Statistics")
        println("5. Display all reviews")
        println("6. Logout")

        when (readlnOrNull()) {
            "1" -> {
                try {
                    println("Enter dish name:")
                    val name = readln()
                    println("Enter price:")
                    val price = readln().toDouble()
                    println("Enter preparation time in seconds:")
                    val prepTime = readln().toInt()

                    val dish = Dish(0, name, price, prepTime)
                    menuService.addDish(dish)
                    println("Dish added successfully.")
                } catch (e: Exception) {
                    println("Error! Invalid input.")
                    println(e.message)
                }
            }
            "2" -> {
                try {
                    println("Enter dish ID to remove:")
                    val dishId = readln().toInt()
                    menuService.removeDish(dishId)
                    println("Dish removed successfully.")
                } catch (e: Exception) {
                    println("Error! You cannot remove this dish.")
                    println(e.message)
                }
            }
            "3" -> {
                println("Most Popular Dishes:")
                statisticsService.getMostPopularDishes().forEach {
                    println("${it.first} - ${it.second} orders")
                }
            }
            "4" -> {
                val now = LocalDateTime.now()
                val startOfMonth = now.withDayOfMonth(1)
                val orderCount = statisticsService.getOrderCount(startOfMonth, now)
                println("Total orders this month: $orderCount")
            }
            "5" -> {
                reviewService.displayAllReviews()
            }
            "6" -> break@loop
            else -> println("Invalid option, please try again.")
        }
    }
}


fun visitorMenu(menuService: MenuService, orderService: OrderService, reviewService: ReviewService, userId: Int) {
    var currentOrderId: Int? = null

    loop@while (true) {
        println("\nVisitor Menu:")
        println("1. View Menu")
        println("2. Create Order")
        println("3. Add Dish to Existing Order")
        println("4. Cancel Order")
        println("5. Pay and Leave Review")
        println("6. View dishes in current orders.")
        println("7. Logout")

        when (readlnOrNull()) {
            "1" -> {
                println("Menu:")
                menuService.getAllDishes().forEach {
                    println("${it.id}: ${it.name} - Price: ${it.price}")
                }
            }
            "2" -> {
                val dishesWithQuantities = getUserDishSelections()
                if (dishesWithQuantities.isNotEmpty()) {
                    currentOrderId = orderService.createOrder(userId, dishesWithQuantities)
                    if (currentOrderId != null) {
                        println("Order created successfully with ID: $currentOrderId")
                    } else {
                        println("Failed to create order.")
                    }
                } else {
                    println("No dishes selected.")
                }
            }
            "3" -> {
                if (currentOrderId == null) {
                    println("No active order found. Please create an order first.")
                    continue@loop
                }
                try {
                    println("Enter dish ID to add:")
                    val dishId = readln().toInt()
                    println("Enter quantity:")
                    val quantity = readln().toInt()
                    orderService.addDishToExistingOrder(currentOrderId, dishId, quantity)
                } catch (e: Exception) {
                    println("Error! Invalid input.")
                    println(e.message)
                }
            }
            "4" -> {
                orderService.cancelAllOrdersForUser(userId)
                println("Your order has been cancelled.")
                currentOrderId = null
            }
            "5" -> {
                if (currentOrderId == null) {
                    println("You don't have an active order.")
                } else {
                    try {
                        println("Enter your rating (1-5):")
                        val rating = readln().toInt()
                        println("Enter your comment:")
                        val comment = readln()
                        reviewService.leaveReview(currentOrderId, userId, rating, comment)
                        println("Thank you for your review.")
                        currentOrderId = null // Assuming the order is complete after payment and review
                    } catch (e: Exception) {
                        println("Error! Invalid input.")
                        println(e.message)
                    }
                }
            }
            "6" -> {
                orderService.viewOrdersForUser(userId)
            }
            "7" -> {
                orderService.completeAllOrdersForUser(userId)
                break@loop
            }
            else -> println("Invalid option, please try again.")
        }
    }
}

fun getUserDishSelections(): List<Pair<Int, Int>> {
    val selections = mutableListOf<Pair<Int, Int>>()

    println("Enter dish ID and quantity (e.g., '1,2' for 2 units of dish ID 1), type 'done' to finish:")
    var input = readlnOrNull()
    while (input != null && input.lowercase() != "done") {
        try {
            val (dishId, quantity) = input.split(",").map { it.trim().toInt() }
            selections.add(Pair(dishId, quantity))
        } catch (e: Exception) {
            println("Invalid format. Please enter as 'dishId,quantity' or type 'done' to finish.")
        }
        input = readlnOrNull()
    }

    return selections
}


