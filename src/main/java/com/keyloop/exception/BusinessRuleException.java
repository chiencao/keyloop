package com.keyloop.exception;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. requested window falls outside dealership opening hours).
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
