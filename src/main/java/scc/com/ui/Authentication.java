package scc.com.ui;

import scc.com.dao.UserDao;
import scc.com.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner scanner){
        this.userDao = userDao;
        this.sc = scanner;
    }

    public void start(){

        System.out.println("====================================");
        System.out.println("Fruit tracking Record System");
        System.out.println("====================================");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.print("Select an Option: ");
        String choice = sc.nextLine();

        switch (choice) {
            case "1":
                handleLogin();
                break;
            case "2":
                System.out.print("Enter new Username: ");
                String newUser = sc.nextLine().trim();
                System.out.print("Enter Password: ");
                String newPass = sc.nextLine();

                System.out.print("Select Role: [1] Admin [2] Dispatcher  [3] Driver:  ");
                String roleChoice = sc.nextLine().trim();
                String role;

                switch (roleChoice) {
                    case "1":
                        role = "ADMIN";
                        break;
                    case "2":
                        role = "DISPATCHER";
                        break;
                    case "3":
                        role = "DRIVER";
                        break;
                    default:
                        role = null;
                        break;
                }
                if (role != null) {
                    userDao.registerUser(newUser, newPass, role);
                } else {
                    System.out.println("Invalid Role Selection!");
                }
                break;
            case "3":
                System.out.printf("Exiting Program. THANK YOU FOR USING THE SYSTEM!");
                break;
        }
    }
    private void handleLogin(){
        System.out.print("Enter Username: ");
        String username = sc.nextLine();
        System.out.print("Enter Password: ");
        String password = sc.nextLine();
        UserDao userDao = new UserDao();

        User user = userDao.login(username, password);

        if (user != null){
            System.out.println(" > Welcome " + user.username() + " | ROLE: " + user.roles());

            switch (user.roles()){
                case "ADMIN" -> new AdminMenu(userDao, sc).display(user);
                default -> System.out.println("Account roles not found!");
            }
        }
    }
}