package scc.com.dao;

import scc.com.model.Fruit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FruitDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    private List<Fruit> query(String sql) {
        List<Fruit> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Fruit(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price_per_kg"),
                        rs.getDouble("stock_kg"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching fruits: " + e.getMessage());
        }
        return list;
    }

    public List<Fruit> getAll() {
        return query("SELECT id, name, price_per_kg, stock_kg, status FROM fruits ORDER BY id");
    }

    public List<Fruit> getActive() {
        return query("SELECT id, name, price_per_kg, stock_kg, status FROM fruits WHERE status = 'ACTIVE' ORDER BY name");
    }

    public boolean addFruit(String name, double pricePerKg, double stockKg, int userId) {
        String sql = "INSERT INTO fruits (name, price_per_kg, stock_kg, status) VALUES (?, ?, ?, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, pricePerKg);
            pstmt.setDouble(3, stockKg);
            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(userId, "FRUIT_ADDED",
                        "User ID " + userId + " added fruit " + name + " (PHP " + pricePerKg + "/kg, stock " + stockKg + " kg)");
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println(" -> [ERROR] A fruit with that name already exists.");
            } else {
                System.err.println("Error adding fruit: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean updatePriceAndStock(int fruitId, double pricePerKg, double stockKg, int userId) {
        String sql = "UPDATE fruits SET price_per_kg = ?, stock_kg = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, pricePerKg);
            pstmt.setDouble(2, stockKg);
            pstmt.setInt(3, fruitId);
            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(userId, "FRUIT_UPDATED",
                        "User ID " + userId + " updated Fruit ID " + fruitId + " to PHP " + pricePerKg + "/kg, stock " + stockKg + " kg");
                return true;
            }
            System.out.println(" -> Fruit ID not found.");
        } catch (SQLException e) {
            System.err.println("Error updating fruit: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStock(int fruitId, double stockKg, int userId) {
        String sql = "UPDATE fruits SET stock_kg = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, stockKg);
            pstmt.setInt(2, fruitId);
            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(userId, "STOCK_UPDATED",
                        "User ID " + userId + " set stock of Fruit ID " + fruitId + " to " + stockKg + " kg");
                return true;
            }
            System.out.println(" -> Fruit ID not found.");
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
        }
        return false;
    }

    // Soft delete: INACTIVE fruits are hidden from customers but old bookings keep their record
    public boolean setStatus(int fruitId, String status, int userId) {
        String sql = "UPDATE fruits SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, fruitId);
            if (pstmt.executeUpdate() > 0) {
                String action = status.equals("ACTIVE") ? "FRUIT_REACTIVATED" : "FRUIT_DEACTIVATED";
                logDao.logAction(userId, action, "Admin ID " + userId + " set Fruit ID " + fruitId + " to " + status);
                return true;
            }
            System.out.println(" -> Fruit ID not found.");
        } catch (SQLException e) {
            System.err.println("Error changing fruit status: " + e.getMessage());
        }
        return false;
    }

    // Sample data so the booking flow can be tested right away (only when the table is empty)
    public void seedDefaultFruits() {
        String check = "SELECT COUNT(*) FROM fruits";
        String insert = "INSERT INTO fruits (name, price_per_kg, stock_kg, status) VALUES (?, ?, ?, 'ACTIVE')";
        Object[][] defaults = {
                {"Mango", 120.0, 100.0},
                {"Banana", 60.0, 150.0},
                {"Pineapple", 80.0, 80.0},
                {"Papaya", 50.0, 60.0},
                {"Watermelon", 45.0, 120.0}
        };
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                    for (Object[] row : defaults) {
                        pstmt.setString(1, (String) row[0]);
                        pstmt.setDouble(2, (Double) row[1]);
                        pstmt.setDouble(3, (Double) row[2]);
                        pstmt.executeUpdate();
                    }
                }
                System.out.println(" -> Sample fruits added <-");
            }
        } catch (SQLException e) {
            System.err.println("Error seeding fruits: " + e.getMessage());
        }
    }
}