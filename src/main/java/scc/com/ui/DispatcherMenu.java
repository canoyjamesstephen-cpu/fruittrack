package scc.com.ui;

import scc.com.dao.ActivityLogDao;
import scc.com.dao.BookingDao;
import scc.com.dao.FruitDao;
import scc.com.dao.UserDao;
import scc.com.model.Booking;
import scc.com.model.Fruit;
import scc.com.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DispatcherMenu {
    private final UserDao userDao;
    private final Scanner sc;
    private final BookingDao bookingDao = new BookingDao();
    private final FruitDao fruitDao = new FruitDao();
    private final ActivityLogDao logDao = new ActivityLogDao();

    public DispatcherMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.sc = scanner;
    }

    public void display(User staff) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n============================");
            System.out.println("    DISPATCHER DASHBOARD    ");
            System.out.println("============================");
            System.out.println("1. View Pending Bookings");
            System.out.println("2. Confirm Booking");
            System.out.println("3. Reject Booking");
            System.out.println("4. Assign / Reassign Driver");
            System.out.println("5. View All Bookings");
            System.out.println("6. Track a Booking");
            System.out.println("7. View Drivers & Workload");
            System.out.println("8. View Fruit Stock");
            System.out.println("9. Update Fruit Stock");
            System.out.println("10. Logout");

            int select = InputUtil.readInt(sc, "Select: ");

            switch (select) {
                case 1 -> Display.bookings(bookingDao.getByStatus("PENDING"), "PENDING BOOKINGS");
                case 2 -> confirmBooking(staff);
                case 3 -> rejectBooking(staff);
                case 4 -> assignDriver(staff);
                case 5 -> Display.bookings(bookingDao.getAll(), "ALL BOOKINGS");
                case 6 -> trackBooking();
                case 7 -> viewDrivers();
                case 8 -> Display.fruits(fruitDao.getAll(), "FRUIT STOCK");
                case 9 -> updateStock(staff);
                case 10 -> {
                    logDao.logAction(staff.id(), "USER_LOGOUT",
                            "User " + staff.username() + " (ID: " + staff.id() + ") logged out.");
                    System.out.println("-> Logging out... Returning to Main Menu.");
                    loggedIn = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void confirmBooking(User staff) {
        List<Booking> pending = bookingDao.getByStatus("PENDING");
        Display.bookings(pending, "PENDING BOOKINGS");
        if (pending.isEmpty()) {
            return;
        }
        int id = InputUtil.readInt(sc, "Enter Booking ID to confirm (or 0 to cancel): ");
        if (id == 0) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        Booking b = pending.stream().filter(x -> x.id() == id).findFirst().orElse(null);
        if (b == null) {
            System.out.println("[ERROR] Booking ID not found.");
            return;
        }
        if (!InputUtil.confirm(sc, "Confirm booking #" + id + " (" + String.format("%.2f", b.quantityKg())
                + " kg " + b.fruitName() + ")? Stock will be deducted")) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        if (bookingDao.confirmBooking(id, staff.id())) {
            System.out.println("-> SUCCESS: Booking #" + id + " confirmed. Assign a driver next.");
        }
    }

    private void rejectBooking(User staff) {
        List<Booking> pending = bookingDao.getByStatus("PENDING");
        Display.bookings(pending, "PENDING BOOKINGS");
        if (pending.isEmpty()) {
            return;
        }
        int id = InputUtil.readInt(sc, "Enter Booking ID to reject (or 0 to cancel): ");
        if (id == 0) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        boolean exists = pending.stream().anyMatch(x -> x.id() == id);
        if (!exists) {
            System.out.println("[ERROR] Booking ID not found.");
            return;
        }
        String reason = InputUtil.readRequired(sc, "Reason for rejecting (or 0 to cancel): ");
        if (reason.equals("0")) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        if (bookingDao.rejectBooking(id, staff.id(), reason)) {
            System.out.println("-> Booking #" + id + " rejected.");
        }
    }

    private void assignDriver(User staff) {
        List<Booking> assignable = new ArrayList<>(bookingDao.getByStatus("CONFIRMED"));
        assignable.addAll(bookingDao.getByStatus("ASSIGNED"));
        Display.bookings(assignable, "READY FOR DRIVER ASSIGNMENT");
        if (assignable.isEmpty()) {
            return;
        }

        int bookingId = InputUtil.readInt(sc, "Enter Booking ID (or 0 to cancel): ");
        if (bookingId == 0) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        boolean exists = assignable.stream().anyMatch(x -> x.id() == bookingId);
        if (!exists) {
            System.out.println("[ERROR] Booking ID not found in the list.");
            return;
        }

        List<User> drivers = userDao.getApprovedDrivers();
        if (drivers.isEmpty()) {
            System.out.println("-> No approved drivers available. Ask the admin to approve a driver first.");
            return;
        }
        viewDrivers();

        int driverId = InputUtil.readInt(sc, "Enter Driver ID (or 0 to cancel): ");
        if (driverId == 0) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        boolean validDriver = drivers.stream().anyMatch(d -> d.id() == driverId);
        if (!validDriver) {
            System.out.println("[ERROR] Driver ID not found.");
            return;
        }
        if (bookingDao.assignDriver(bookingId, driverId, staff.id())) {
            System.out.println("-> SUCCESS: Driver ID " + driverId + " assigned to booking #" + bookingId + ".");
        }
    }

    private void trackBooking() {
        int id = InputUtil.readInt(sc, "Enter Booking ID to track (or 0 to cancel): ");
        if (id == 0) {
            return;
        }
        Booking b = bookingDao.getById(id);
        if (b == null) {
            System.out.println("[ERROR] Booking ID not found.");
            return;
        }
        Display.bookingDetails(b, bookingDao.getTracking(id));
    }

    private void viewDrivers() {
        List<User> drivers = userDao.getApprovedDrivers();
        System.out.println("=============== DRIVERS & WORKLOAD ===============");
        if (drivers.isEmpty()) {
            System.out.println("-> No approved drivers.");
            return;
        }
        String border = Display.border(3, 15, 17);
        System.out.println(border);
        System.out.printf("| %-3s | %-15s | %-17s |%n", "ID", "Driver", "Active Deliveries");
        System.out.println(border);
        for (User d : drivers) {
            System.out.printf("| %-3d | %-15s | %-17d |%n", d.id(), d.username(), bookingDao.countActiveForDriver(d.id()));
        }
        System.out.println(border);
    }

    private void updateStock(User staff) {
        List<Fruit> fruits = fruitDao.getAll();
        Display.fruits(fruits, "FRUIT STOCK");
        if (fruits.isEmpty()) {
            return;
        }
        int id = InputUtil.readInt(sc, "Enter Fruit ID to update stock (or 0 to cancel): ");
        if (id == 0) {
            System.out.println("-> Operation Cancelled.");
            return;
        }
        boolean exists = fruits.stream().anyMatch(f -> f.id() == id);
        if (!exists) {
            System.out.println("[ERROR] Fruit ID not found.");
            return;
        }
        double stock = InputUtil.readDouble(sc, "Enter new stock in kg: ");
        if (fruitDao.updateStock(id, stock, staff.id())) {
            System.out.println("-> SUCCESS: Stock updated to " + String.format("%.2f", stock) + " kg.");
        }
    }
}