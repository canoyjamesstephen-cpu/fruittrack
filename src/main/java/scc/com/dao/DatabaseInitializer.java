package scc.com.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        createUsersTable();      // ADDED: users table is created if the DB file is new
        createTables();
        createDeliveryTables();  // ADDED: fruits, bookings, booking_tracking
        new UserDao();
        new FruitDao().seedDefaultFruits(); // ADDED: sample fruits (only if table is empty)
        System.out.println(" -> Database tables verified.");
    }

    private static void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }

    // ADDED
    private static void createUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                roles TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING'
            );
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing users table: " + e.getMessage());
        }
    }

    // ADDED
    private static void createDeliveryTables() {
        String fruits = """
            CREATE TABLE IF NOT EXISTS fruits (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                price_per_kg REAL NOT NULL CHECK (price_per_kg >= 0),
                stock_kg REAL NOT NULL DEFAULT 0 CHECK (stock_kg >= 0),
                status TEXT NOT NULL DEFAULT 'ACTIVE'
            );
            """;
        String bookings = """
            CREATE TABLE IF NOT EXISTS bookings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL,
                fruit_id INTEGER NOT NULL,
                quantity_kg REAL NOT NULL CHECK (quantity_kg > 0),
                address TEXT NOT NULL,
                total_price REAL NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                driver_id INTEGER,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (customer_id) REFERENCES users(id),
                FOREIGN KEY (fruit_id) REFERENCES fruits(id),
                FOREIGN KEY (driver_id) REFERENCES users(id)
            );
            """;
        String tracking = """
            CREATE TABLE IF NOT EXISTS booking_tracking (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                booking_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                note TEXT,
                updated_by INTEGER NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
                FOREIGN KEY (updated_by) REFERENCES users(id)
            );
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(fruits);
            stmt.execute(bookings);
            stmt.execute(tracking);
        } catch (SQLException e) {
            System.err.println("Error initializing delivery tables: " + e.getMessage());
        }
    }
}