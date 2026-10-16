package scc.com.ui;

import scc.com.model.User;

import java.util.Scanner;

/**
 * UI layer - screen shown after a successful login.
 * Choosing Logout ends the session and returns to the main menu.
 */
public class UserMenu {

    private final Scanner scanner;
    private final User user;

    public UserMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- USER MENU ---");
            System.out.println("Logged in as: " + user.username()
                    + " | Role: " + user.roles()
                    + " | Status: " + user.status());
            System.out.println("1. View my profile");
            System.out.println("2. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    System.out.println("ID       : " + user.id());
                    System.out.println("Username : " + user.username());
                    System.out.println("Role     : " + user.roles());
                    System.out.println("Status   : " + user.status());
                }
                case "2" -> {
                    System.out.println("You have been logged out.");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}