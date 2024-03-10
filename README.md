# Restaurant Management System with Kotlin and PostgreSQL

This project is a server-side application developed in Kotlin, utilizing coroutines for efficient asynchronous operations and backed by a PostgreSQL database. It features functionalities for managing orders, dishes, users, and providing real-time updates on order statuses.

## Database Setup

Initiate the application's PostgreSQL database by executing the SQL commands below:

```sql
CREATE TYPE user_role AS ENUM ('ADMIN', 'VISITOR');

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    role user_role NOT NULL
);

CREATE TABLE dishes (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    preparation_time INT NOT NULL
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_details (
    order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
    dish_id INTEGER REFERENCES dishes(id),
    quantity INTEGER NOT NULL,
    PRIMARY KEY (order_id, dish_id)
);

CREATE TABLE reviews (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    user_id INTEGER REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

## Configuration

Modify the `DatabaseConfig` object in your Kotlin application to match your PostgreSQL connection details:

```kotlin
object DatabaseConfig {
    private const val url = "jdbc:postgresql://localhost:5432/your_database_name" // Your database URL
    private const val user = "your_username" // Your database username
    private const val password = "your_password" // Your database password
}
```

Replace `"your_database_name"`, `"your_username"`, and `"your_password"` with your actual PostgreSQL database name, username, and password.

## Running the Application

- Make sure Kotlin and PostgreSQL are installed on your system.
- Prepare the PostgreSQL database as outlined in the Database Setup section.
- Adjust your database connection details in the `DatabaseConfig` class as described.
- Compile and run the application through `src/main/kotlin/ui/UserInterface.kt` using your preferred method, e.g., through an IDE or a build tool like Gradle.

## Launching Locally

1. Clone the repository to your local environment.
2. Ensure your PostgreSQL server is operational, and the database is configured as instructed above.
3. Update the `DatabaseConfig` class with your PostgreSQL connection details.
4. Execute the application in your development environment or via the command line using a build tool.

## Additional Information

- **Design Patterns**:
    - The **Singleton** pattern is utilized for efficient database connection management.
    - The **Repository** pattern abstracts database access within OrderRepository, DishRepository, and ReviewRepository.
    - Integration of the **Observer** pattern allows for real-time user notifications about their order status updates.
- **Asynchronous Processing**: Leveraging Kotlin Coroutines across the application for tasks such as order processing and status notifications significantly boosts performance and responsiveness.
