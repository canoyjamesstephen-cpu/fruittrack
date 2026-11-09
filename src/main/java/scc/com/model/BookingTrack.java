package scc.com.model;

public record BookingTrack(
        int id,
        int bookingId,
        String status,
        String note,
        String updatedBy,
        String timestamp
) {}