package com.example.keyloop.service;

import com.example.keyloop.exception.InvalidNameException;
import com.example.keyloop.model.Greeting;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds the greeting business logic. Kept free of web concerns so it can be
 * unit tested in isolation (see GreetingServiceTest).
 */
@Service
public class GreetingService {

    private static final String TEMPLATE = "Hello, %s!";

    private final AtomicLong counter = new AtomicLong();

    public Greeting greet(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidNameException("name must not be blank");
        }
        String trimmed = name.trim();
        return new Greeting(counter.incrementAndGet(), String.format(TEMPLATE, trimmed));
    }
}
