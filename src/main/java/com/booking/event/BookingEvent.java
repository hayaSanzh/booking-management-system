package com.booking.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published to RabbitMQ when a booking is created or canceled.
 * Consumed by notification-service for sending notifications.
 */
public class BookingEvent {

    private String eventId;
    private EventType eventType;
    private Long bookingId;
    private String userEmail;
    private String userFullName;
    private String resourceName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public enum EventType {
        BOOKING_CREATED,
        BOOKING_CANCELED
    }

    public BookingEvent() {
    }

    public static BookingEvent created(Long bookingId, String userEmail, String userFullName,
                                        String resourceName, LocalDateTime startAt, LocalDateTime endAt) {
        BookingEvent event = new BookingEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(EventType.BOOKING_CREATED);
        event.setBookingId(bookingId);
        event.setUserEmail(userEmail);
        event.setUserFullName(userFullName);
        event.setResourceName(resourceName);
        event.setStartAt(startAt);
        event.setEndAt(endAt);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static BookingEvent canceled(Long bookingId, String userEmail, String userFullName,
                                         String resourceName, LocalDateTime startAt, LocalDateTime endAt) {
        BookingEvent event = new BookingEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(EventType.BOOKING_CANCELED);
        event.setBookingId(bookingId);
        event.setUserEmail(userEmail);
        event.setUserFullName(userFullName);
        event.setResourceName(resourceName);
        event.setStartAt(startAt);
        event.setEndAt(endAt);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "BookingEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType=" + eventType +
                ", bookingId=" + bookingId +
                ", userEmail='" + userEmail + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", startAt=" + startAt +
                '}';
    }
}
