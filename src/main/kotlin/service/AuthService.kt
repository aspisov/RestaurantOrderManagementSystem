package service

import db.DatabaseConfig
import model.User
import model.UserRole
import org.mindrot.jbcrypt.BCrypt

class AuthService {
    fun registerUser(username: String, password: String, role: UserRole) {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        DatabaseConfig.getConnection().use { conn ->
            val stmt = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?::user_role)")
            stmt.setString(1, username)
            stmt.setString(2, hashedPassword)
            stmt.setString(3, role.name)
            stmt.executeUpdate()
        }
    }

    fun authenticateUser(username: String, password: String): User? {
        DatabaseConfig.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT id, username, role, password FROM users WHERE username = ?")
            stmt.setString(1, username)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val hashedPasswordFromDb = rs.getString("password")
                if (BCrypt.checkpw(password, hashedPasswordFromDb)) {
                    // Здесь передается пустая строка вместо реального или хешированного пароля
                    return User(rs.getInt("id"), rs.getString("username"), UserRole.valueOf(rs.getString("role")))
                }
            }
        }
        return null
    }

}
