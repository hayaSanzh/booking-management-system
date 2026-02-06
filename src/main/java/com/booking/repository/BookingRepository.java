package com.booking.repository;

import com.booking.entity.Booking;
import com.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    /**
     * Check for overlapping bookings on the same resource.
     * Overlap condition: new.startAt < existing.endAt AND new.endAt > existing.startAt
     * Only checks non-canceled bookings.
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.resource.id = :resourceId " +
           "AND b.status <> 'CANCELED' " +
           "AND b.startAt < :endAt " +
           "AND b.endAt > :startAt")
    boolean existsOverlappingBooking(
            @Param("resourceId") Long resourceId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    /**
     * Find overlapping bookings (for detailed conflict info)
     */
    @Query("SELECT b FROM Booking b " +
           "WHERE b.resource.id = :resourceId " +
           "AND b.status <> 'CANCELED' " +
           "AND b.startAt < :endAt " +
           "AND b.endAt > :startAt")
    List<Booking> findOverlappingBookings(
            @Param("resourceId") Long resourceId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    /**
     * Find all bookings by user
     */
    List<Booking> findByUserIdOrderByStartAtDesc(UUID userId);

    /**
     * Find bookings by resource and status
     */
    List<Booking> findByResourceIdAndStatus(Long resourceId, BookingStatus status);
}
