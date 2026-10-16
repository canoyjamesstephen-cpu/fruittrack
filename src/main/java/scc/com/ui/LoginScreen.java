package scc.com.ui;

import scc.com.dao.UserDao;
import scc.com.model.User;

import java.util.Optional;
import java.util.Scanner;

/**
 * UI layer - Login screen.
 */
public class LoginScreen {

    private final Scanner scanner;
    private final UserDao userDao;

    public LoginScreen(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void show() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<User> result = userDao.login(username, password);

        if (result.isEmpty()) {
            System.out.println("Invalid username or password.");
            return;
        }

        User user = result.get();
        System.out.println("Welcome, " + user.username() + "!");
        new UserMenu(scanner, user).show();
    }
}