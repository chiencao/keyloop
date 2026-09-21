package com.example.keyloop.model;

/**
 * Immutable response payload returned by the greeting endpoint.
 */
public record Greeting(long id, String content) {
}
