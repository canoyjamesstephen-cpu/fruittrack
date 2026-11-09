package scc.com.ui;

import java.util.Scanner;

public class InputUtil {

    public static int readInt(Scanner sc, String prompt) {
        while (true) {
            if (!prompt.isEmpty()) {
                System.out.print(prompt);
            }
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number.");
            }
        }
    }

    // Accepts 0 or any positive number
    public static double readDouble(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                    System.out.println("-> [ERROR] Value cannot be negative.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Invalid input. Please enter a number.");
            }
        }
    }

    // Keeps asking until the person types something (they can type 0 and the caller treats it as cancel)
    public static String readRequired(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("-> [ERROR] This field cannot be empty.");
        }
    }

    public static boolean confirm(Scanner sc, String question) {
        System.out.print(question + " (Y/N): ");
        return sc.nextLine().trim().equalsIgnoreCase("Y");
    }
}