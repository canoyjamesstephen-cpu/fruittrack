package scc.com.ui;

import scc.com.dao.ActivityLogDao;
import scc.com.dao.BookingDao;
import scc.com.dao.FruitDao;
import scc.com.dao.UserDao;
import scc.com.model.Booking;
import scc.com.model.Fruit;
import scc.com.model.User;

import java.util.List;
import java.util.Scanner;

public class CustomerMenu {
    private final UserDao userDao;
    private final Scanner sc;
    private final FruitDao fruitDao = new FruitDao();
    private final BookingDao bookingDao = new BookingDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public CustomerMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.sc = scanner;
    }

    public void display(User customer) {
        User current = customer;
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n============================");
            System.out.println("     CUSTOMER DASHBOARD     ");
            System.out.println("============================");
            System.out.println("Welcome, " + current.username());
            System.out.println("1. Request Fruit Transport");
            System.out.println("2. Track My Shipments");
            System.out.println("3. Cancel a Pending Request");
            System.out.println("4. View / Edit My Profile");
            System.out.println("5. Logout");

            int select = InputUtil.readInt(sc, "Select: ");

            switch (select) {
                case 1 -> requestTransport(current);
                case 2 -> trackShipments(current);
                case 3 -> cancelRequest(current);
                case 4 -> current = profile(current);
                case 5 -> {
                    logDao.logAction(current.id(), "USER_LOGOUT",
                            "User " + current.username() + " (ID: " + current.id() + ") logged out.");
                    System.out.println("-> Logging out... Returning to Main Menu.");
                    loggedIn = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    // 1. Request Fruit Transport
    private void requestTransport(User customer) {
        List<Fruit> available = fruitDao.getActive().stream().filter(f -> f.stockKg() > 0).toList();
        Display.fruits(available, "AVAILABLE FRUITS");
        if (available.isEmpty()) {
            return;
        }

        int fruitId = InputUtil.readInt(sc, "Enter Fruit ID to order (or 0 to cancel): ");
        if (fruitId == 0) {
            System.out.println("-> Request Cancelled.");
            return;
        }
        Fruit fruit = available.stream().filter(f -> f.id() == fruitId).findFirst().orElse(null);
        if (fruit == null) {
            System.out.println("[ERROR] Fruit ID not found.");
            return;
        }

        double qty;
        while (true) {
            qty = InputUtil.readDouble(sc, "Quantity in kg (max " + String.format("%.2f", fruit.stockKg()) + ", or 0 to cancel): ");
            if (qty == 0) {
                System.out.println("-> Request Cancelled.");
                return;
            }
            if (qty > fruit.stockKg()) {
                System.out.println("-> [ERROR] Only " + String.format("%.2f", fruit.stockKg()) + " kg available.");
                continue;
            }
            break;
        }

        String address = InputUtil.readRequired(sc, "Delivery address (or 0 to cancel): ");
        if (address.equals("0")) {
            System.out.println("-> Request Cancelled.");
            return;
        }

        double total = Math.round(fruit.pricePerKg() * qty * 100.0) / 100.0;
        System.out.println("\n--- Request Summary ---");
        System.out.println("Fruit   : " + fruit.name() + " (PHP " + String.format("%.2f", fruit.pricePerKg()) + "/kg)");
        System.out.println("Quantity: " + String.format("%.2f", qty) + " kg");
        System.out.println("Total   : PHP " + String.format("%.2f", total));
        System.out.println("Address : " + address);

        if (!InputUtil.confirm(sc, "Submit this request?")) {
            System.out.println("-> Request Cancelled.");
            return;
        }

        int newId = bookingDao.createBooking(customer.id(), fruit.id(), qty, address);
        if (newId > 0) {
            System.out.println("-> SUCCESS: Request #" + newId + " submitted. Waiting for dispatcher confirmation.");
        } else {
            System.out.println("-> [ERROR] Request failed.");
        }
    }

    // 2. Track My Shipments
    private void trackShipments(User customer) {
        List<Booking> mine = bookingDao.getByCustomer(customer.id());
        Display.bookings(mine, "MY SHIPMENTS");
        if (mine.isEmpty()) {
            return;
        }
        int id = InputUtil.readInt(sc, "Enter Request ID to see tracking (or 0 to go back): ");
        if (id == 0) {
            return;
        }
        Booking b = mine.stream().filter(x -> x.id() == id).findFirst().orElse(null);
        if (b == null) {
            System.out.println("[ERROR] Request ID not found.");
            return;
        }
        Display.bookingDetails(b, bookingDao.getTracking(id));
    }

    // 3. Cancel a Pending Request
    private void cancelRequest(User customer) {
        List<Booking> pending = bookingDao.getByCustomer(customer.id()).stream()
                .filter(b -> b.status().equals("PENDING")).toList();
        Display.bookings(pending, "PENDING REQUESTS (can be cancelled)");
        if (pending.isEmpty()) {
            return;
        }
        int id = InputUtil.readInt(sc, "Enter Request ID to cancel (or 0 to go back): ");
        if (id == 0) {
            return;
        }
        boolean exists = pending.stream().anyMatch(b -> b.id() == id);
        if (!exists) {
            System.out.println("[ERROR] Request ID not found.");
            return;
        }
        if (!InputUtil.confirm(sc, "Cancel request #" + id + "?")) {
            System.out.println("-> Cancel aborted.");
            return;
        }
        if (bookingDao.cancelByCustomer(id, customer.id())) {
            System.out.println("-> SUCCESS: Request #" + id + " cancelled.");
        }
    }

    // 4. View / Edit My Profile (returns the refreshed user)
    private User profile(User customer) {
        User current = customer;
        boolean back = false;

        while (!back) {
            System.out.println("\n--- MY PROFILE ---");
            System.out.println("User ID  : " + current.id());
            System.out.println("Username : " + current.username());
            System.out.println("Role     : " + current.roles());
            System.out.println("Status   : " + current.status());
            System.out.println("\n1. Change Username");
            System.out.println("2. Change Password");
            System.out.println("0. Back");

            int option = InputUtil.readInt(sc, "Select: ");

            switch (option) {
                case 1 -> {
                    String newName = InputUtil.readRequired(sc, "New username (or 0 to cancel): ");
                    if (newName.equals("0")) {
                        System.out.println("-> Cancelled.");
                    } else if (newName.contains(" ")) {
                        System.out.println("-> [ERROR] Username cannot contain spaces.");
                    } else if (userDao.updateUsername(current.id(), newName)) {
                        System.out.println("-> SUCCESS: Username updated.");
                        User refreshed = userDao.getUserById(current.id());
                        if (refreshed != null) {
                            current = refreshed;
                        }
                    }
                }
                case 2 -> {
                    System.out.print("Current password (or 0 to cancel): ");
                    String oldPass = sc.nextLine();
                    if (oldPass.equals("0")) {
                        System.out.println("-> Cancelled.");
                    } else if (!oldPass.equals(current.password())) {
                        System.out.println("-> [ERROR] Current password is incorrect.");
                    } else {
                        String newPass = InputUtil.readRequired(sc, "New password: ");
                        System.out.print("Confirm new password: ");
                        String again = sc.nextLine();
                        if (!newPass.equals(again)) {
                            System.out.println("-> [ERROR] Passwords do not match.");
                        } else if (userDao.updatePassword(current.id(), newPass)) {
                            System.out.println("-> SUCCESS: Password updated.");
                            User refreshed = userDao.getUserById(current.id());
                            if (refreshed != null) {
                                current = refreshed;
                            }
                        }
                    }
                }
                case 0 -> back = true;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
        return current;
    }
}
