package com.example.keyloop.exception;

/**
 * Thrown when a caller supplies a name that fails business validation.
 */
public class InvalidNameException extends RuntimeException {

    public InvalidNameException(String message) {
        super(message);
    }
}
