package com.booking.repository;

import com.booking.entity.Booking;
import com.booking.entity.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.UUID;

public class BookingSpecifications {

    public static Specification<Booking> hasResourceId(Long resourceId) {
        return (root, query, cb) -> {
            if (resourceId == null) return null;
            return cb.equal(root.get("resource").get("id"), resourceId);
        };
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Booking> hasUserId(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) return null;
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Booking> startsAfter(LocalDateTime dateFrom) {
        return (root, query, cb) -> {
            if (dateFrom == null) return null;
            return cb.greaterThanOrEqualTo(root.get("startAt"), dateFrom);
        };
    }

    public static Specification<Booking> endsBefore(LocalDateTime dateTo) {
        return (root, query, cb) -> {
            if (dateTo == null) return null;
            return cb.lessThanOrEqualTo(root.get("endAt"), dateTo);
        };
    }

    /**
     * Filter bookings that overlap with a given time range
     */
    public static Specification<Booking> overlapsWithRange(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null || to == null) return null;
            // Overlap: booking.startAt < to AND booking.endAt > from
            return cb.and(
                cb.lessThan(root.get("startAt"), to),
                cb.greaterThan(root.get("endAt"), from)
            );
        };
    }
}
