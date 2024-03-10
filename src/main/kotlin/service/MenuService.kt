package service

import db.DatabaseConfig
import model.Dish

class MenuService {
    fun addDish(dish: Dish) {
        DatabaseConfig.getConnection().use { conn ->
            conn.prepareStatement("INSERT INTO dishes (name, price, preparation_time) VALUES (?, ?, ?)").apply {
                setString(1, dish.name)
                setDouble(2, dish.price)
                setInt(3, dish.preparationTime)
                executeUpdate()
            }
        }
    }

    fun removeDish(dishId: Int) {
        DatabaseConfig.getConnection().use { conn ->
            conn.prepareStatement("DELETE FROM dishes WHERE id = ?").apply {
                setInt(1, dishId)
                executeUpdate()
            }
        }
    }

    fun getAllDishes(): List<Dish> {
        val dishes = mutableListOf<Dish>()
        DatabaseConfig.getConnection().use { conn ->
            conn.createStatement().executeQuery("SELECT id, name, price, preparation_time FROM dishes").use { rs ->
                while (rs.next()) {
                    dishes.add(Dish(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        price = rs.getDouble("price"),
                        preparationTime = rs.getInt("preparation_time")
                    ))
                }
            }
        }
        return dishes
    }
}
