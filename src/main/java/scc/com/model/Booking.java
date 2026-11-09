package scc.com.model;

public record Booking(
        int id,
        int customerId,
        String customerName,
        int fruitId,
        String fruitName,
        double quantityKg,
        String address,
        double totalPrice,
        String status,
        int driverId,          // 0 = no driver assigned yet
        String driverName,     // "-" = no driver assigned yet
        String createdAt
) {}