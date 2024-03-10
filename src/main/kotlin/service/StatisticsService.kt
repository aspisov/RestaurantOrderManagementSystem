package service

import db.DatabaseConfig
import java.sql.Timestamp
import java.time.LocalDateTime

class StatisticsService {
    fun getMostPopularDishes(): List<Pair<String, Int>> {
        val popularDishes = mutableListOf<Pair<String, Int>>()
        DatabaseConfig.getConnection().use { conn ->
            val query = """
                SELECT d.name, COUNT(od.dish_id) AS dish_count 
                FROM order_details od 
                JOIN dishes d ON od.dish_id = d.id 
                GROUP BY d.name 
                ORDER BY dish_count DESC 
                LIMIT 10
            """.trimIndent()
            conn.prepareStatement(query).executeQuery().use { rs ->
                while (rs.next()) {
                    val dishName = rs.getString("name")
                    val count = rs.getInt("dish_count")
                    popularDishes.add(Pair(dishName, count))
                }
            }
        }
        return popularDishes
    }

    fun getOrderCount(startDate: LocalDateTime, endDate: LocalDateTime): Int {
        DatabaseConfig.getConnection().use { conn ->
            val query = """
                SELECT COUNT(*) AS order_count 
                FROM orders 
                WHERE created_at BETWEEN ? AND ?
            """.trimIndent()
            conn.prepareStatement(query).apply {
                setTimestamp(1, Timestamp.valueOf(startDate))
                setTimestamp(2, Timestamp.valueOf(endDate))
            }.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getInt("order_count")
                }
            }
        }
        return 0
    }
}
