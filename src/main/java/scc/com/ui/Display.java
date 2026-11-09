package scc.com.ui;

import scc.com.model.Booking;
import scc.com.model.BookingTrack;
import scc.com.model.Fruit;

import java.util.List;

// Shared table printing for the fruit / booking screens
public class Display {

    private static String cut(String s, int max) {
        if (s == null) {
            return "-";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }

    public static String border(int... widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        return sb.toString();
    }

    public static void fruits(List<Fruit> fruits, String title) {
        System.out.println("=============== " + title + " ===============");
        if (fruits.isEmpty()) {
            System.out.println("-> No Fruits Found");
            return;
        }
        String border = border(3, 20, 10, 10, 8);
        System.out.println(border);
        System.out.printf("| %-3s | %-20s | %-10s | %-10s | %-8s |%n", "ID", "Name", "Price/kg", "Stock(kg)", "Status");
        System.out.println(border);
        for (Fruit f : fruits) {
            System.out.printf("| %-3d | %-20s | %10.2f | %10.2f | %-8s |%n",
                    f.id(), cut(f.name(), 20), f.pricePerKg(), f.stockKg(), f.status());
        }
        System.out.println(border);
    }

    public static void bookings(List<Booking> bookings, String title) {
        System.out.println("=============== " + title + " ===============");
        if (bookings.isEmpty()) {
            System.out.println("-> No Bookings Found");
            return;
        }
        String border = border(4, 12, 12, 8, 10, 10, 12, 19);
        System.out.println(border);
        System.out.printf("| %-4s | %-12s | %-12s | %-8s | %-10s | %-10s | %-12s | %-19s |%n",
                "ID", "Customer", "Fruit", "Kg", "Total", "Status", "Driver", "Created");
        System.out.println(border);
        for (Booking b : bookings) {
            System.out.printf("| %-4d | %-12s | %-12s | %8.2f | %10.2f | %-10s | %-12s | %-19s |%n",
                    b.id(), cut(b.customerName(), 12), cut(b.fruitName(), 12), b.quantityKg(),
                    b.totalPrice(), b.status(), cut(b.driverName(), 12), cut(b.createdAt(), 19));
        }
        System.out.println(border);
    }

    public static void bookingDetails(Booking b, List<BookingTrack> tracks) {
        System.out.println("\n--------- BOOKING #" + b.id() + " ---------");
        System.out.println("Customer : " + b.customerName());
        System.out.println("Fruit    : " + b.fruitName() + " x " + String.format("%.2f", b.quantityKg()) + " kg");
        System.out.println("Total    : PHP " + String.format("%.2f", b.totalPrice()));
        System.out.println("Address  : " + b.address());
        System.out.println("Driver   : " + b.driverName());
        System.out.println("Status   : " + b.status());
        System.out.println("\nTracking history:");
        if (tracks.isEmpty()) {
            System.out.println("  (no updates yet)");
        }
        for (BookingTrack t : tracks) {
            System.out.println("  [" + t.timestamp() + "] " + t.status() + " - " + t.note() + " (by " + t.updatedBy() + ")");
        }
    }
}