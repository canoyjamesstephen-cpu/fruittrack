package scc.com.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {


    private static final String DB_FILE = "activity_logs fruittrack.db";

    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;
    private static boolean pathPrinted = false;

    public static Connection getConnection() throws SQLException {

        if (!pathPrinted) {
            File f = new File(DB_FILE);
            System.out.println("[DB] Using file: " + f.getAbsolutePath()
                    + (f.exists() ? "" : "  (WARNING: file does not exist yet, new one will be created!)"));
            pathPrinted = true;
        }

        Connection conn = DriverManager.getConnection(DB_URL);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}