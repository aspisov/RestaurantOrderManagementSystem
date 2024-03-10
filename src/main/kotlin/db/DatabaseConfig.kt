package db

import java.sql.Connection
import java.sql.DriverManager

object DatabaseConfig {
    private const val url = "jdbc:postgresql://localhost:5432/restaurant"
    private const val user = "raspri"
    private const val password = "password"

    fun getConnection(): Connection {
        return DriverManager.getConnection(url, user, password)
    }
}