package scc.com.ui;

import scc.com.dao.ActivityLogDao;
import scc.com.dao.UserDao;
import scc.com.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;
    private final ActivityLogDao logDao = new ActivityLogDao();

    public Authentication(UserDao userDao, Scanner scanner){
        this.userDao = userDao;
        this.sc = scanner;
    }

    public void start(){
        boolean running = true;

        while (running) {
            System.out.println("\n====================================");
            System.out.println("     Fruit tracking Record System     ");
            System.out.println("====================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Select an Option: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> handleLogin();
                case "2" -> {
                    System.out.print("Enter new Username: ");
                    String newUser = sc.nextLine().trim();
                    System.out.print("Enter Password: ");
                    String newPass = sc.nextLine();

                    System.out.print("Select Role: [1] Admin [2] Dispatcher [3] Driver [4] Customer: ");
                    String roleChoice = sc.nextLine().trim();
                    String role;

                    switch (roleChoice) {
                        case "1" -> role = "ADMIN";
                        case "2" -> role = "DISPATCHER";
                        case "3" -> role = "DRIVER";
                        case "4" -> role = "CUSTOMER"; // ADDED
                        default -> role = null;
                    }
                    if (role != null) {
                        if (role.equals("CUSTOMER")) {
                            userDao.registerCustomer(newUser, newPass); // ADDED: customers are auto-approved
                        } else {
                            userDao.registerUser(newUser, newPass, role);
                        }
                    } else {
                        System.out.println("-> [ERROR] Invalid Role Selection!");
                    }
                }
                case "3" -> {
                    System.out.println("Exiting Program. THANK YOU FOR USING THE SYSTEM!");
                    running = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option. Please choose 1, 2, or 3.");
            }
        }
    }

    private void handleLogin(){
        System.out.print("Enter Username: ");
        String username = sc.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();

        User user = userDao.login(username, password);

        if (user != null){
            System.out.println(" > Welcome " + user.username() + " | ROLE: " + user.roles());

            // Log successful login
            logDao.logAction(user.id(), "USER_LOGIN", "User " + user.username() + " (ID: " + user.id() + ") logged in.");
            switch (user.roles()){
                case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
                case "DISPATCHER" -> new DispatcherMenu(userDao, sc).display(user); // ADDED
                case "DRIVER" -> new DriverMenu(sc).display(user);                  // ADDED
                case "CUSTOMER" -> new CustomerMenu(userDao, sc).display(user);     // ADDED
                default -> System.out.println("-> Account roles not found or unauthorized view!");
            }
        }
    }
}