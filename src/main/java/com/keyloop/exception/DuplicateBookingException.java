package com.keyloop.exception;

/**
 * Thrown when the requested vehicle already has a CONFIRMED appointment that
 * overlaps the requested window — a vehicle can only be serviced in one place at
 * a time. Maps to HTTP 409 Conflict.
 */
public class DuplicateBookingException extends RuntimeException {

    public DuplicateBookingException(String message) {
        super(message);
    }
}
