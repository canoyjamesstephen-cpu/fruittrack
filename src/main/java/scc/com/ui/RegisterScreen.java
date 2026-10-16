package scc.com.ui;

import scc.com.dao.UserDao;

import java.util.Scanner;

/**
 * UI layer - Register Account screen.
 */
public class RegisterScreen {

    private final Scanner scanner;
    private final UserDao userDao;

    public RegisterScreen(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void show() {
        System.out.println("\n--- REGISTER ACCOUNT ---");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty.");
            return;
        }
        if (userDao.usernameExists(username)) {
            System.out.println("Username already taken.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        String role = chooseRole();

        if (userDao.register(username, password, role)) {
            System.out.println("Registered as " + role + ". Status: PENDING (waiting for approval).");
        }
    }

    private String chooseRole() {
        while (true) {
            System.out.println("Select role:");
            System.out.println("1. ADMIN");
            System.out.println("2. DISPATCHER");
            System.out.println("3. DRIVER");
            System.out.println("4. CUSTOMER");
            System.out.print("Choose: ");
            switch (scanner.nextLine().trim()) {
                case "1": return "ADMIN";
                case "2": return "DISPATCHER";
                case "3": return "DRIVER";
                case "4": return "CUSTOMER";
                default: System.out.println("Invalid choice.");
            }
        }
    }
}