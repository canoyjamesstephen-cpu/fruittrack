package scc.com.dao;

import scc.com.model.Booking;
import scc.com.model.BookingTrack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookingDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    private static final String BASE_SELECT = """
            SELECT b.id, b.customer_id, c.username AS customer_name,
                   b.fruit_id, f.name AS fruit_name,
                   b.quantity_kg, b.address, b.total_price, b.status,
                   b.driver_id, d.username AS driver_name, b.created_at
            FROM bookings b
            JOIN users c ON c.id = b.customer_id
            JOIN fruits f ON f.id = b.fruit_id
            LEFT JOIN users d ON d.id = b.driver_id
            """;

    // ---------- helpers ----------

    @FunctionalInterface
    private interface TxWork<T> {
        T run(Connection conn) throws SQLException;
    }

    // Runs everything in one transaction. NOTE: activity-log writes are done AFTER commit
    // (a second connection cannot write while this one holds the write lock).
    private <T> T inTransaction(TxWork<T> work, T onError) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return onError;
        }
    }

    private Booking map(ResultSet rs) throws SQLException {
        String driverName = rs.getString("driver_name");
        return new Booking(
                rs.getInt("id"),
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                rs.getInt("fruit_id"),
                rs.getString("fruit_name"),
                rs.getDouble("quantity_kg"),
                rs.getString("address"),
                rs.getDouble("total_price"),
                rs.getString("status"),
                rs.getInt("driver_id"),
                driverName == null ? "-" : driverName,
                rs.getString("created_at")
        );
    }

    private List<Booking> query(String suffix, Object... params) {
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(BASE_SELECT + suffix)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching bookings: " + e.getMessage());
        }
        return list;
    }

    private String statusOf(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT status FROM bookings WHERE id = ?")) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private void setStatus(Connection conn, int bookingId, String status) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE bookings SET status = ? WHERE id = ?")) {
            pstmt.setString(1, status);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
        }
    }

    private void addTracking(Connection conn, int bookingId, String status, String note, int userId) throws SQLException {
        String sql = "INSERT INTO booking_tracking (booking_id, status, note, updated_by) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            pstmt.setString(2, status);
            pstmt.setString(3, note);
            pstmt.setInt(4, userId);
            pstmt.executeUpdate();
        }
    }

    // ---------- reads ----------

    public List<Booking> getAll() {
        return query(" ORDER BY b.id DESC");
    }

    public List<Booking> getByStatus(String status) {
        return query(" WHERE b.status = ? ORDER BY b.id DESC", status);
    }

    public List<Booking> getByCustomer(int customerId) {
        return query(" WHERE b.customer_id = ? ORDER BY b.id DESC", customerId);
    }

    public List<Booking> getByDriver(int driverId) {
        return query(" WHERE b.driver_id = ? ORDER BY b.id DESC", driverId);
    }

    public List<Booking> getActiveByDriver(int driverId) {
        return query(" WHERE b.driver_id = ? AND b.status IN ('ASSIGNED','ON_THE_WAY') ORDER BY b.id", driverId);
    }

    public Booking getById(int bookingId) {
        List<Booking> list = query(" WHERE b.id = ?", bookingId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int countActiveForDriver(int driverId) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE driver_id = ? AND status IN ('ASSIGNED','ON_THE_WAY')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, driverId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting deliveries: " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getStatusCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : new String[]{"PENDING", "CONFIRMED", "ASSIGNED", "ON_THE_WAY", "DELIVERED", "FAILED", "CANCELLED"}) {
            counts.put(s, 0);
        }
        String sql = "SELECT status, COUNT(*) FROM bookings GROUP BY status";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                counts.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching summary: " + e.getMessage());
        }
        return counts;
    }

    public List<BookingTrack> getTracking(int bookingId) {
        List<BookingTrack> list = new ArrayList<>();
        String sql = "SELECT t.id, t.booking_id, t.status, t.note, u.username, t.timestamp " +
                "FROM booking_tracking t JOIN users u ON u.id = t.updated_by " +
                "WHERE t.booking_id = ? ORDER BY t.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new BookingTrack(
                            rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6)));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tracking: " + e.getMessage());
        }
        return list;
    }

    // ---------- customer ----------

    // Returns the new booking ID, or -1 on failure
    public int createBooking(int customerId, int fruitId, double quantityKg, String address) {
        int newId = inTransaction(conn -> {
            double price = 0;
            double stock = 0;
            String fruitStatus = null;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT price_per_kg, stock_kg, status FROM fruits WHERE id = ?")) {
                pstmt.setInt(1, fruitId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(" -> Fruit not found.");
                        return -1;
                    }
                    price = rs.getDouble(1);
                    stock = rs.getDouble(2);
                    fruitStatus = rs.getString(3);
                }
            }
            if (!"ACTIVE".equals(fruitStatus)) {
                System.out.println(" -> This fruit is no longer available.");
                return -1;
            }
            if (quantityKg <= 0 || quantityKg > stock) {
                System.out.println(" -> Not enough stock. Available: " + stock + " kg.");
                return -1;
            }
            double total = Math.round(price * quantityKg * 100.0) / 100.0;

            int id = -1;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO bookings (customer_id, fruit_id, quantity_kg, address, total_price, status) " +
                            "VALUES (?, ?, ?, ?, ?, 'PENDING')", Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, customerId);
                pstmt.setInt(2, fruitId);
                pstmt.setDouble(3, quantityKg);
                pstmt.setString(4, address);
                pstmt.setDouble(5, total);
                pstmt.executeUpdate();
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("No booking ID returned");
                    }
                    id = keys.getInt(1);
                }
            }
            addTracking(conn, id, "PENDING", "Booking placed by customer", customerId);
            return id;
        }, -1);

        if (newId > 0) {
            logDao.logAction(customerId, "BOOKING_CREATED",
                    "Customer ID " + customerId + " placed booking #" + newId);
        }
        return newId;
    }

    // Customers may only cancel their own bookings while still PENDING
    public boolean cancelByCustomer(int bookingId, int customerId) {
        boolean ok = inTransaction(conn -> {
            int owner = 0;
            String status = null;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT customer_id, status FROM bookings WHERE id = ?")) {
                pstmt.setInt(1, bookingId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        owner = rs.getInt(1);
                        status = rs.getString(2);
                    }
                }
            }
            if (status == null || owner != customerId) {
                System.out.println(" -> Booking not found.");
                return false;
            }
            if (!"PENDING".equals(status)) {
                System.out.println(" -> Only PENDING bookings can be cancelled (current: " + status
                        + "). Please contact the dispatcher.");
                return false;
            }
            setStatus(conn, bookingId, "CANCELLED");
            addTracking(conn, bookingId, "CANCELLED", "Cancelled by customer", customerId);
            return true;
        }, false);

        if (ok) {
            logDao.logAction(customerId, "BOOKING_CANCELLED",
                    "Customer ID " + customerId + " cancelled booking #" + bookingId);
        }
        return ok;
    }

    // ---------- dispatcher / staff ----------

    // PENDING -> CONFIRMED, and the stock is deducted here
    public boolean confirmBooking(int bookingId, int staffId) {
        boolean ok = inTransaction(conn -> {
            int fruitId = 0;
            double qty = 0;
            String status = null;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT fruit_id, quantity_kg, status FROM bookings WHERE id = ?")) {
                pstmt.setInt(1, bookingId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        fruitId = rs.getInt(1);
                        qty = rs.getDouble(2);
                        status = rs.getString(3);
                    }
                }
            }
            if (status == null) {
                System.out.println(" -> Booking not found.");
                return false;
            }
            if (!"PENDING".equals(status)) {
                System.out.println(" -> Only PENDING bookings can be confirmed (current: " + status + ").");
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE fruits SET stock_kg = stock_kg - ? WHERE id = ? AND stock_kg >= ?")) {
                pstmt.setDouble(1, qty);
                pstmt.setInt(2, fruitId);
                pstmt.setDouble(3, qty);
                if (pstmt.executeUpdate() == 0) {
                    System.out.println(" -> Not enough stock to confirm this booking.");
                    return false;
                }
            }
            setStatus(conn, bookingId, "CONFIRMED");
            addTracking(conn, bookingId, "CONFIRMED", "Booking confirmed, stock reserved", staffId);
            return true;
        }, false);

        if (ok) {
            logDao.logAction(staffId, "BOOKING_CONFIRMED",
                    "User ID " + staffId + " confirmed booking #" + bookingId);
        }
        return ok;
    }

    // PENDING -> CANCELLED (rejected by staff)
    public boolean rejectBooking(int bookingId, int staffId, String reason) {
        boolean ok = inTransaction(conn -> {
            String status = statusOf(conn, bookingId);
            if (status == null) {
                System.out.println(" -> Booking not found.");
                return false;
            }
            if (!"PENDING".equals(status)) {
                System.out.println(" -> Only PENDING bookings can be rejected (current: " + status + ").");
                return false;
            }
            setStatus(conn, bookingId, "CANCELLED");
            addTracking(conn, bookingId, "CANCELLED", "Rejected by dispatcher: " + reason, staffId);
            return true;
        }, false);

        if (ok) {
            logDao.logAction(staffId, "BOOKING_REJECTED",
                    "User ID " + staffId + " rejected booking #" + bookingId + " (" + reason + ")");
        }
        return ok;
    }

    // CONFIRMED / ASSIGNED -> ASSIGNED (assign or re-assign a driver)
    public boolean assignDriver(int bookingId, int driverId, int staffId) {
        boolean ok = inTransaction(conn -> {
            String status = statusOf(conn, bookingId);
            if (status == null) {
                System.out.println(" -> Booking not found.");
                return false;
            }
            if (!"CONFIRMED".equals(status) && !"ASSIGNED".equals(status)) {
                System.out.println(" -> A driver can only be assigned to CONFIRMED or ASSIGNED bookings (current: "
                        + status + ").");
                return false;
            }
            String driverName = null;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT username FROM users WHERE id = ? AND roles = 'DRIVER' AND status = 'APPROVED'")) {
                pstmt.setInt(1, driverId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        driverName = rs.getString(1);
                    }
                }
            }
            if (driverName == null) {
                System.out.println(" -> That user is not an approved driver.");
                return false;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE bookings SET driver_id = ?, status = 'ASSIGNED' WHERE id = ?")) {
                pstmt.setInt(1, driverId);
                pstmt.setInt(2, bookingId);
                pstmt.executeUpdate();
            }
            addTracking(conn, bookingId, "ASSIGNED", "Assigned to driver " + driverName, staffId);
            return true;
        }, false);

        if (ok) {
            logDao.logAction(staffId, "DRIVER_ASSIGNED",
                    "User ID " + staffId + " assigned Driver ID " + driverId + " to booking #" + bookingId);
        }
        return ok;
    }

    // ---------- driver ----------

    // Allowed: ASSIGNED -> ON_THE_WAY, ON_THE_WAY -> DELIVERED or FAILED. Only the assigned driver can do it.
    public boolean updateDeliveryStatus(int bookingId, int driverId, String newStatus, String note) {
        boolean ok = inTransaction(conn -> {
            int assignedDriver = 0;
            String status = null;
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT driver_id, status FROM bookings WHERE id = ?")) {
                pstmt.setInt(1, bookingId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        assignedDriver = rs.getInt(1);
                        status = rs.getString(2);
                    }
                }
            }
            if (status == null || assignedDriver != driverId) {
                System.out.println(" -> Booking not found in your assigned deliveries.");
                return false;
            }
            boolean validMove =
                    ("ASSIGNED".equals(status) && "ON_THE_WAY".equals(newStatus))
                            || ("ON_THE_WAY".equals(status)
                            && ("DELIVERED".equals(newStatus) || "FAILED".equals(newStatus)));
            if (!validMove) {
                System.out.println(" -> Invalid status change: " + status + " -> " + newStatus + ".");
                return false;
            }
            setStatus(conn, bookingId, newStatus);
            addTracking(conn, bookingId, newStatus, note, driverId);
            return true;
        }, false);

        if (ok) {
            logDao.logAction(driverId, "DELIVERY_" + newStatus,
                    "Driver ID " + driverId + " set booking #" + bookingId + " to " + newStatus);
        }
        return ok;
    }
}