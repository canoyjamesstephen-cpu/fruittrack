package scc.com.ui;

import scc.com.dao.UserDao;
import scc.com.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final UserDao userDao;
    private final Scanner scanner;

    // NEW: allowed values for validation
    private static final List<String> VALID_ROLES = List.of("ADMIN", "DISPATCHER", "DRIVER");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("============================");
            System.out.println("       ADMIN DASHBOARD      ");
            System.out.println("============================");
            System.out.println("1. View All User");
            System.out.println("2. View Pending Users");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add User");
            System.out.println("5. Update User role / Status");
            System.out.println("6. Delete User");
            System.out.println("7. Logout");
            System.out.print("Select: ");

            int select = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (select) {
                case 1 -> viewAllUser();
                case 2 -> viewPendingUser();
                case 3 -> approveUser();
                case 5 -> updateUser(admin); // NEW
                case 7 -> loggedIn = false;
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

    private void approveUser(){
        List<User> pendingUsers = userDao.getPendingUser();
        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()){
            return;
        }
        System.out.print("Enter User ID to Approve (or 0 to cancel): ");
        int userID = scanner.nextInt();
        if (userID == 0){
            System.out.println("-> Operation Cancelled by the User");
        }
        boolean exits = pendingUsers.stream().anyMatch(u -> u.id() ==userID );
        if (!exits){
            System.out.println("[ERROR] User ID not found.");
            return;
        }
        userDao.approveUser(userID);
    }

    // ---------------------------------------------------------------
    // NEW: Update user role / status
    // ---------------------------------------------------------------
    private void updateUser(User currentAdmin) {
        List<User> users = userDao.getAllUsers();
        displayUserTable(users, "ALL REGISTERED USER");

        if (users.isEmpty()) {
            return;
        }

        // Step 1: target user ID
        int targetUserId = readIntInput("Enter User ID to update (or 0 to cancel): ");
        if (targetUserId == 0) {
            System.out.println("-> Update Cancelled.");
            return;
        }

        // Self-edit restriction
        if (targetUserId == currentAdmin.id()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == targetUserId);
        if (!exists) {
            System.out.println("[ERROR] User ID not found.");
            return;
        }

        // Step 2: new role
        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("-> Update Cancelled.");
            return;
        }

        // Step 3: new status
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("-> Update Cancelled.");
            return;
        }

        // Step 4: save
        boolean updated = userDao.updateUserRoleAndStatus(targetUserId, newRole, newStatus);
        if (updated) {
            System.out.println("-> SUCCESS: User ID " + targetUserId + " is now " + newRole + " / " + newStatus + ".");
        } else {
            System.out.println("-> [ERROR] Update failed.");
        }
    }

    // ---------------------------------------------------------------
    // NEW: Input validation wrappers
    // ---------------------------------------------------------------

    // Keeps asking until the user types a whole number (no crash on letters/symbols)
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number.");
            }
        }
    }

    // Returns a valid role in uppercase, or null if the user enters 0 to cancel
    private String readValidRole() {
        while (true) {
            System.out.print("Select Role: [1] Admin [2] Dispatcher [3] Driver (or 0 to cancel): ");
            String roleChoice = scanner.nextLine().trim();

            switch (roleChoice) {
                case "0" -> {
                    return null;
                }
                case "1" -> {
                    return VALID_ROLES.get(0); // ADMIN
                }
                case "2" -> {
                    return VALID_ROLES.get(1); // DISPATCHER
                }
                case "3" -> {
                    return VALID_ROLES.get(2); // DRIVER
                }
                default -> System.out.println("-> [ERROR] Invalid role. Choose 1, 2, or 3.");
            }
        }
    }

    // Returns a valid status in uppercase, or null if the user enters 0 to cancel
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