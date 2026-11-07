package scc.com.ui;

import scc.com.dao.ActivityLogDao;
import scc.com.dao.UserDao;
import scc.com.model.ActivityLog;
import scc.com.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;
    private final ActivityLogDao logDao = new ActivityLogDao();

    private static final List<String> VALID_ROLES = List.of("ADMIN", "DISPATCHER", "DRIVER");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n============================");
            System.out.println("       ADMIN DASHBOARD      ");
            System.out.println("============================");
            System.out.println("1. View All User");
            System.out.println("2. View Pending Users");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add User");
            System.out.println("5. Update User role / Status");
            System.out.println("6. Delete User");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Logout");
            System.out.print("Select: ");

            int select = readIntInput("");

            switch (select) {
                case 1 -> viewAllUser();
                case 2 -> viewPendingUser();
                case 3 -> approveUser(admin);
                case 4 -> addUser(admin); // ADDED
                case 5 -> updateUser(admin);
                case 6 -> deleteUser(admin); // FIXED: was DeleteUser()
                case 7 -> viewAuditLogs();
                case 8 -> {
                    // ADDED: Log the logout action into the database
                    logDao.logAction(admin.id(), "USER_LOGOUT", "User " + admin.username() + " (ID: " + admin.id() + ") logged out.");

                    System.out.println("-> Logging out... Returning to Main Menu.");
                    loggedIn = false; // Breaks loop, goes back to Authentication menu
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void displayUserTable(List<User> users, String title) {
        System.out.println("===================================" + title + "===================================");
        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }
        String border = "+-----+-----------------+---------------------------+------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-15s | %-25s | %-10s |\n", "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User u : users) {
            System.out.printf("| %-3s | %-15s | %-25s | %-10s |\n",
                    u.id(), u.username(), u.roles(), u.status());
        }
        System.out.println(border);
    }

    private void viewAllUser() {
        displayUserTable(userDao.getAllUsers(), "ALL REGISTERED USER");
    }

    private void viewPendingUser(){
        displayUserTable(userDao.getPendingUser(), "PENDING APPROVALS");
    }

    private void approveUser(User admin){
        List<User> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()){
            return;
        }
        int userID = readIntInput("Enter User ID to Approve (or 0 to cancel): ");
        if (userID == 0){
            System.out.println("-> Operation Cancelled by the User");
            return;
        }
        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userID);
        if (!exists){
            System.out.println("[ERROR] User ID not found.");
            return;
        }
        userDao.approveUser(userID, admin.id());
    }

    // ADDED: Add user (admin creates an account directly)
    private void addUser(User currentAdmin) {
        System.out.println("\n--- Add New User (type 0 to cancel) ---");

        List<User> users = userDao.getAllUsers();

        // Username
        String username;
        while (true) {
            System.out.print("Enter Username: ");
            username = scanner.nextLine().trim();

            if (username.equals("0")) {
                System.out.println("-> Add User Cancelled.");
                return;
            }
            if (username.isEmpty()) {
                System.out.println("-> [ERROR] Username cannot be empty.");
                continue;
            }
            if (username.contains(" ")) {
                System.out.println("-> [ERROR] Username cannot contain spaces.");
                continue;
            }
            final String check = username;
            boolean taken = users.stream().anyMatch(u -> u.username().equalsIgnoreCase(check));
            if (taken) {
                System.out.println("-> [ERROR] Username already exists. Try another one.");
                continue;
            }
            break;
        }

        // Password
        String password;
        while (true) {
            System.out.print("Enter Password: ");
            password = scanner.nextLine();
            if (password.isEmpty()) {
                System.out.println("-> [ERROR] Password cannot be empty.");
                continue;
            }
            break;
        }

        // Role
        String role = readValidRole();
        if (role == null) {
            System.out.println("-> Add User Cancelled.");
            return;
        }

        // Status
        String status = readValidStatus();
        if (status == null) {
            System.out.println("-> Add User Cancelled.");
            return;
        }

        boolean added = userDao.addUser(username, password, role, status, currentAdmin.id());
        if (added) {
            System.out.println("-> SUCCESS: User '" + username + "' added as " + role + " / " + status + ".");
        } else {
            System.out.println("-> [ERROR] Failed to add user.");
        }
    }

    private void updateUser(User currentAdmin) {
        List<User> users = userDao.getAllUsers();
        displayUserTable(users, "ALL REGISTERED USER");

        if (users.isEmpty()) {
            return;
        }

        int targetUserId = readIntInput("Enter User ID to update (or 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("-> Update Cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == targetUserId);
        if (!exists) {
            System.out.println("[ERROR] User ID not found.");
            return;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("-> Update Cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("-> Update Cancelled.");
            return;
        }

        boolean updated = userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus, currentAdmin.id());
        if (updated) {
            System.out.println("-> SUCCESS: User ID " + targetUserId + " is now " + newRole + " / " + newStatus + ".");
        } else {
            System.out.println("-> [ERROR] Update failed.");
        }
    }

    // ADDED: Delete user
    private void deleteUser(User currentAdmin) {
        List<User> users = userDao.getAllUsers();
        displayUserTable(users, "ALL REGISTERED USER");

        if (users.isEmpty()) {
            return;
        }

        int targetUserId = readIntInput("Enter User ID to delete (or 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("-> Delete Cancelled.");
            return;
        }

        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: You cannot delete the currently active admin account.");
            return;
        }

        User target = users.stream()
                .filter(u -> u.id() == targetUserId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println("[ERROR] User ID not found.");
            return;
        }

        // ADDED: Prevent deleting the last remaining admin
        if (target.roles().toString().equalsIgnoreCase("ADMIN")) {
            long adminCount = users.stream()
                    .filter(u -> u.roles().toString().equalsIgnoreCase("ADMIN"))
                    .count();
            if (adminCount <= 1) {
                System.out.println("[ERROR] Cannot delete the last remaining admin.");
                return;
            }
        }

        System.out.print("Are you sure you want to delete '" + target.username() + "'? (Y/N): ");
        String confirm = scanner.nextLine().trim().toUpperCase();
        if (!confirm.equals("Y")) {
            System.out.println("-> Delete Cancelled.");
            return;
        }

        boolean deleted = userDao.deleteUser(targetUserId);
        if (deleted) {
            logDao.logAction(currentAdmin.id(), "USER_DELETED",
                    "Admin " + currentAdmin.username() + " deleted user " + target.username() + " (ID: " + targetUserId + ").");
            System.out.println("-> SUCCESS: User '" + target.username() + "' has been deleted.");
        } else {
            System.out.println("-> [ERROR] Delete failed.");
        }
    }

    private void viewAuditLogs() {
        System.out.println("\n--- Audit Log Menu ---");
        System.out.println("1. View All System Logs");
        System.out.println("2. Filter Logs by User ID");
        int option = readIntInput("Select option: ");

        if (option == 1) {
            displayLogsTable(logDao.getAllLogs());
        } else if (option == 2) {
            int filterId = readIntInput("Enter User ID to filter: ");
            displayLogsTable(logDao.getLogsByUserId(filterId));
        } else {
            System.out.println("-> [ERROR] Invalid choice.");
        }
    }

    private void displayLogsTable(List<ActivityLog> logs) {
        System.out.println("==========================================================================================================");
        if (logs.isEmpty()) {
            System.out.println("-> No Audit Logs Found.");
            return;
        }
        String border = "+-------+----------+--------------------+-------------------------------------+---------------------+";
        System.out.println(border);
        System.out.printf("| %-5s | %-8s | %-18s | %-35s | %-19s |\n", "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(border);
        for (ActivityLog log : logs) {
            System.out.printf("| %-5d | %-8d | %-18s | %-35s | %-19s |\n",
                    log.id(), log.userId(), log.actionType(), log.description(), log.timestamp());
        }
        System.out.println(border);
    }

    private int readIntInput(String prompt) {
        while (true) {
            if (!prompt.isEmpty()) {
                System.out.print(prompt);
            }
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number.");
            }
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Select Role: [1] Admin [2] Dispatcher [3] Driver (or 0 to cancel): ");
            String roleChoice = scanner.nextLine().trim();

            switch (roleChoice) {
                case "0" -> { return null; }
                case "1" -> { return VALID_ROLES.get(0); }
                case "2" -> { return VALID_ROLES.get(1); }
                case "3" -> { return VALID_ROLES.get(2); }
                default -> System.out.println("-> [ERROR] Invalid role. Choose 1, 2, or 3.");
            }
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED / PENDING, or 0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("-> [ERROR] Invalid status. Choose APPROVED or PENDING.");
        }
    }
}