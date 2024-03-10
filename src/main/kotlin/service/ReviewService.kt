package service

import db.DatabaseConfig
import java.sql.Timestamp
import java.time.LocalDateTime

class ReviewService {
    fun leaveReview(orderId: Int, userId: Int, rating: Int, comment: String) {
        DatabaseConfig.getConnection().use { conn ->
            val stmt = conn.prepareStatement("""
                INSERT INTO reviews (order_id, user_id, rating, comment, created_at) 
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent())
            stmt.setInt(1, orderId)
            stmt.setInt(2, userId)
            stmt.setInt(3, rating)
            stmt.setString(4, comment)
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()))
            stmt.executeUpdate()

            println("Review successfully added.")
        }
    }

    fun displayAllReviews() {
        DatabaseConfig.getConnection().use { conn ->
            val stmt = conn.prepareStatement("""
            SELECT r.id, r.order_id, r.user_id, r.rating, r.comment, r.created_at, u.username
            FROM reviews r
            JOIN users u ON r.user_id = u.id
            ORDER BY r.created_at DESC
        """.trimIndent())

            val resultSet = stmt.executeQuery()

            if (!resultSet.next()) {
                println("No reviews found.")
                return
            } else {
                println("Displaying all reviews:")
                do {
                    val id = resultSet.getInt("id")
                    val orderId = resultSet.getInt("order_id")
                    val userId = resultSet.getInt("user_id")
                    val username = resultSet.getString("username")
                    val rating = resultSet.getInt("rating")
                    val comment = resultSet.getString("comment")
                    val createdAt = resultSet.getTimestamp("created_at").toLocalDateTime()

                    println("Review ID: $id | Order ID: $orderId | User ID: $userId ($username) | Rating: $rating | Comment: $comment | Date: $createdAt")
                } while (resultSet.next())
            }
        }
    }

}
