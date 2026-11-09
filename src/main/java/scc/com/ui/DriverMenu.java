package scc.com.ui;

import scc.com.dao.ActivityLogDao;
import scc.com.dao.BookingDao;
import scc.com.model.Booking;
import scc.com.model.User;

import java.util.List;
import java.util.Scanner;

public class DriverMenu {
    private final Scanner sc;
    private final BookingDao bookingDao = new BookingDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public DriverMenu(Scanner scanner) {
        this.sc = scanner;
    }

    public void display(User driver) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n============================");
            System.out.println("       DRIVER DASHBOARD     ");
            System.out.println("============================");
            System.out.println("Welcome, " + driver.username());
            System.out.println("1. My Active Deliveries");
            System.out.println("2. Update Delivery Status");
            System.out.println("3. Delivery History");
            System.out.println("4. Logout");

            int select = InputUtil.readInt(sc, "Select: ");

            switch (select) {
                case 1 -> viewActive(driver);
                case 2 -> updateDelivery(driver);
                case 3 -> Display.bookings(bookingDao.getByDriver(driver.id()), "MY DELIVERY HISTORY");
                case 4 -> {
                    logDao.logAction(driver.id(), "USER_LOGOUT",
                            "User " + driver.username() + " (ID: " + driver.id() + ") logged out.");
                    System.out.println("-> Logging out... Returning to Main Menu.");
                    loggedIn = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void viewActive(User driver) {
        List<Booking> active = bookingDao.getActiveByDriver(driver.id());
        Display.bookings(active, "MY ACTIVE DELIVERIES");
        for (Booking b : active) {
            System.out.println("  #" + b.id() + " -> " + b.address());
        }
    }

    private void updateDelivery(User driver) {
        List<Booking> active = bookingDao.getActiveByDriver(driver.id());
        Display.bookings(active, "MY ACTIVE DELIVERIES");
        if (active.isEmpty()) {
            return;
        }

        int id = InputUtil.readInt(sc, "Enter Booking ID to update (or 0 to cancel): ");
        if (id == 0) {
            System.out.println("-> Update Cancelled.");
            return;
        }
        Booking b = active.stream().filter(x -> x.id() == id).findFirst().orElse(null);
        if (b == null) {
            System.out.println("[ERROR] Booking ID not found in your active deliveries.");
            return;
        }

        System.out.println("Deliver to: " + b.address());

        if (b.status().equals("ASSIGNED")) {
            if (!InputUtil.confirm(sc, "Start delivery for booking #" + id + "?")) {
                System.out.println("-> Update Cancelled.");
                return;
            }
            if (bookingDao.updateDeliveryStatus(id, driver.id(), "ON_THE_WAY", "Driver is on the way")) {
                System.out.println("-> SUCCESS: Booking #" + id + " is now ON_THE_WAY.");
            }
        } else {
            System.out.println("[1] Mark as DELIVERED   [2] Report FAILED delivery   [0] Cancel");
            int choice = InputUtil.readInt(sc, "Select: ");
            switch (choice) {
                case 1 -> {
                    if (bookingDao.updateDeliveryStatus(id, driver.id(), "DELIVERED", "Delivered to customer")) {
                        System.out.println("-> SUCCESS: Booking #" + id + " marked as DELIVERED.");
                    }
                }
                case 2 -> {
                    String reason = InputUtil.readRequired(sc, "Reason for failed delivery (or 0 to cancel): ");
                    if (reason.equals("0")) {
                        System.out.println("-> Update Cancelled.");
                        return;
                    }
                    if (bookingDao.updateDeliveryStatus(id, driver.id(), "FAILED", "Delivery failed: " + reason)) {
                        System.out.println("-> Booking #" + id + " marked as FAILED.");
                    }
                }
                case 0 -> System.out.println("-> Update Cancelled.");
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }
}