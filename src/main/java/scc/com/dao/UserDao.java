package scc.com.dao;

import java.sql.Connection;

import scc.com.model.User;
import scc.com.dao.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao logDao = new ActivityLogDao(); // ADDED

    public UserDao() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error default seeding admin: " + e.getMessage());
        }
    }

    public void defaultAdminGenerator(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE roles = 'ADMIN'";
        String insertSql = "INSERT INTO users (username, password, roles, status) VALUES('admin','admin123','ADMIN','APPROVED')";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(insertSql);
                System.out.println(" -> Default Admin created <-");
            }
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String password, String role) {
        String sql = "INSERT INTO users(username, password, roles, status) VALUES(?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.toUpperCase());
            pstmt.executeUpdate();
            System.out.println("SUCCESSFUL REGISTRATION, waiting for approval.");
            return true;
        } catch (SQLException e) {
            System.err.println("FAILED: " + e.getMessage());
            return false;
        }
    }

    // ADDED: customers register themselves and are approved automatically
    public boolean registerCustomer(String username, String password) {
        String sql = "INSERT INTO users(username, password, roles, status) VALUES(?, ?, 'CUSTOMER', 'APPROVED')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("SUCCESSFUL REGISTRATION. You can now login as a customer.");
            return true;
        } catch (SQLException e) {
            System.err.println("FAILED: " + e.getMessage());
            return false;
        }
    }

    public User login(String username, String password) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    if ("PENDING".equalsIgnoreCase(status)) {
                        System.out.println("-> Account is pending. Waiting for approval.");
                        return null;
                    }
                    // ADDED: deactivated accounts cannot log in
                    if ("DEACTIVATED".equalsIgnoreCase(status)) {
                        System.out.println("-> Account is deactivated. Please contact the administrator.");
                        return null;
                    }
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            status
                    );
                } else {
                    System.out.println(" > Invalid Username or Password");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error During login: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> usersList = new ArrayList<>();
        String sql = "SELECT id, username, password, roles, status FROM users";

        try(Connection conn = DatabaseConnection.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){
            while (rs.next()){
                usersList.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e){
            System.err.println("Error fetching all users: " + e.getMessage());
        }

        return usersList;
    }

    public List<User> getPendingUser(){
        List<User> pendingList = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status = 'PENDING'";

        try(Connection conn = DatabaseConnection.getConnection()){
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()){
                pendingList.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }
        }catch (SQLException e){
            System.out.println("Error Fetching User: "+e.getMessage());
        }
        return pendingList;
    }

    // ADDED: drivers that dispatchers can assign deliveries to
    public List<User> getApprovedDrivers() {
        List<User> drivers = new ArrayList<>();
        String sql = "SELECT id, username, password, roles, status FROM users WHERE roles = 'DRIVER' AND status = 'APPROVED' ORDER BY username";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                drivers.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching drivers: " + e.getMessage());
        }
        return drivers;
    }

    // ADDED: used by the customer profile to refresh the logged-in user
    public User getUserById(int userId) {
        String sql = "SELECT id, username, password, roles, status FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"),
                            rs.getString("roles"), rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }
        return null;
    }

    // UPDATED: Added adminId parameter to record audit trail
    public boolean approveUser(int userID, int adminId){
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";

        try(Connection conn = DatabaseConnection.getConnection()){
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, userID);
            int rowsUpdated = pstmt.executeUpdate();

            if(rowsUpdated > 0 ){
                System.out.println(" ->USER ID: "+userID+" APPROVED SUCCESSFULLY!");
                // ADDED: Log Action Trigger
                logDao.logAction(adminId, "USER_APPROVED", "Admin ID " + adminId + " approved account for User ID " + userID);
                return true;
            }else {
                System.out.println("ID NOT FOUND.");
            }
        }catch (SQLException e){
            System.out.println("Error Fetching User: "+e.getMessage());
        }
        return false;
    }

    // UPDATED: Added adminId parameter to record audit trail
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus, int adminId) {
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.toUpperCase());
            pstmt.setString(2, newStatus.toUpperCase());
            pstmt.setInt(3, userId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println(" -> USER ID: " + userId + " UPDATED SUCCESSFULLY!");
                // ADDED: Log Action Trigger
                logDao.logAction(adminId, "USER_UPDATED", "Admin ID " + adminId + " updated User ID " + userId + " role to " + newRole + " and status to " + newStatus);
                return true;
            } else {
                System.out.println(" -> ID NOT FOUND.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
        return false;
    }

    // ADDED: used for deactivate (DEACTIVATED) and reactivate (APPROVED); the record is kept, not deleted
    public boolean setUserStatus(int userId, String newStatus, int adminId, String actionType) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, userId);

            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(adminId, actionType,
                        "Admin ID " + adminId + " set User ID " + userId + " status to " + newStatus);
                return true;
            }
            System.out.println(" -> ID NOT FOUND.");
        } catch (SQLException e) {
            System.err.println("Error changing user status: " + e.getMessage());
        }
        return false;
    }

    // ADDED: customer profile - change username
    public boolean updateUsername(int userId, String newUsername) {
        String sql = "UPDATE users SET username = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername);
            pstmt.setInt(2, userId);
            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(userId, "PROFILE_UPDATED", "User ID " + userId + " changed username to " + newUsername);
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.out.println(" -> [ERROR] Username already exists. Try another one.");
            } else {
                System.err.println("Error updating username: " + e.getMessage());
            }
        }
        return false;
    }

    // ADDED: customer profile - change password
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);
            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(userId, "PROFILE_UPDATED", "User ID " + userId + " changed password");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        return false;
    }

    // ADDED: Admin creates a user directly (with chosen role and status)
    public boolean addUser(String username, String password, String role, String status, int adminId) {
        String sql = "INSERT INTO users(username, password, roles, status) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.toUpperCase());
            pstmt.setString(4, status.toUpperCase());

            if (pstmt.executeUpdate() > 0) {
                logDao.logAction(adminId, "USER_ADDED",
                        "Admin ID " + adminId + " added user " + username + " as " + role + " / " + status);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
        }
        return false;
    }

    // ADDED: Delete user (audit log is recorded in AdminMenu.deleteUser)
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            int rowsDeleted = pstmt.executeUpdate();

            if (rowsDeleted > 0) {
                return true;
            } else {
                System.out.println(" -> ID NOT FOUND.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            // ADDED: hint when the user already has booking records
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
                System.out.println(" -> This user already has booking records. Use Deactivate instead of Delete.");
            }
        }
        return false;
    }
}