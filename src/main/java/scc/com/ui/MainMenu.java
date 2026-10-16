package scc.com.ui;

import scc.com.dao.UserDao;

import java.util.Scanner;

/**
 * UI layer - the first screen (before login).
 */
public class MainMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final UserDao userDao = new UserDao();

    private final RegisterScreen registerScreen = new RegisterScreen(scanner, userDao);
    private final LoginScreen loginScreen = new LoginScreen(scanner, userDao);

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n==============================");
            System.out.println(" FRUITTRACK - Fruit Delivery");
            System.out.println("==============================");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> registerScreen.show();
                case "2" -> loginScreen.show();
                case "3" -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }
}