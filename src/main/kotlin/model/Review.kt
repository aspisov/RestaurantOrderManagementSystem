package model

data class Review(val id: Int, val orderId: Int, val userId: Int, val rating: Int, val comment: String, val createdAt: java.time.LocalDateTime)