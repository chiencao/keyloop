package com.keyloop.exception;

/** Thrown when a referenced entity (dealership, vehicle, service type...) does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
