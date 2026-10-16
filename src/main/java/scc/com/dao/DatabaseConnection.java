package scc.com.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO layer - Singleton helper that owns the ONE SQLite connection
 * and creates the users table on first run.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:fruittrack.db";

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() { }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    public void initialize() {
        String createSql = """
                CREATE TABLE IF NOT EXISTS users (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles    TEXT NOT NULL CHECK (roles IN ('ADMIN', 'DISPATCHER', 'DRIVER', 'CUSTOMER')),
                    status   TEXT NOT NULL DEFAULT 'PENDING'
                )
                """;
        try (Statement stmt = getConnection().createStatement()) {
            if (hasOldRoles(stmt)) {
                // Old table still uses TEACHER/STUDENT, so rebuild it.
                stmt.execute("DROP TABLE users");
            }
            stmt.execute(createSql);
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Returns true if the existing users table still has the old TEACHER/STUDENT roles. */
    private boolean hasOldRoles(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'")) {
            if (rs.next()) {
                String sql = rs.getString("sql");
                return sql != null && sql.contains("TEACHER");
            }
        }
        return false;
    }
}