package scc.com;

import scc.com.dao.DatabaseInitializer;
import scc.com.dao.UserDao;
import scc.com.ui.Authentication;


import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        Scanner scanner = new Scanner(System.in);
        UserDao userDao = new UserDao();

        Authentication auth = new Authentication(userDao, scanner);
        auth.start();
        scanner.close();
    }
}