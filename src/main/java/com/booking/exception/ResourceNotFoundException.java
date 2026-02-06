package com.booking.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    
    public ResourceNotFoundException(String resourceName, Object id) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", 
              String.format("%s not found with id: %s", resourceName, id));
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
