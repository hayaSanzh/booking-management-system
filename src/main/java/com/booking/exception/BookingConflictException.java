package com.booking.exception;

import org.springframework.http.HttpStatus;

public class BookingConflictException extends ApiException {

    public BookingConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public BookingConflictException(Long resourceId) {
        super(HttpStatus.CONFLICT, "Booking conflict: the requested time slot overlaps with an existing booking for resource " + resourceId);
    }
}
