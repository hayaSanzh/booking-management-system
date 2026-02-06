package com.booking.exception;

import org.springframework.http.HttpStatus;

public class BookingValidationException extends ApiException {

    public BookingValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
