package com.keyloop.exception;

/**
 * Thrown when no technician and/or service bay can satisfy the requested
 * window. Maps to HTTP 409 Conflict.
 */
public class NoAvailabilityException extends RuntimeException {

    public NoAvailabilityException(String message) {
        super(message);
    }
}
