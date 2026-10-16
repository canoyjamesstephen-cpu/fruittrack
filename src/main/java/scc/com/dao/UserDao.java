package scc.com.dao;

import scc.com.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * DAO layer - every SQL query for the "users" table lives here.
 */
public class UserDao {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public boolean register(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, 'PENDING')";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            ps.setString(3, role);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Register failed: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
            return false;
        }
    }

    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            rs.getString("status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    private String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}