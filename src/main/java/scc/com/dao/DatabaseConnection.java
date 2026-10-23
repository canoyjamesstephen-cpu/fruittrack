package scc.com.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:sqlite:fruittrack.db";

    public static Connection getConnection() throws SQLException{
        Connection conn = DriverManager.getConnection(DB_URL);

        try(Statement stmt = conn.createStatement()){
            stmt.execute("PRAGMA foreign_keys =  ON ");
        }
        return  conn;
    }
}