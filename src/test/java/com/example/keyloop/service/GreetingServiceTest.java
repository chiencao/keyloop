package com.example.keyloop.service;

import com.example.keyloop.exception.InvalidNameException;
import com.example.keyloop.model.Greeting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test for the service layer: no Spring context is started, so it
 * runs in milliseconds.
 */
class GreetingServiceTest {

    private GreetingService service;

    @BeforeEach
    void setUp() {
        service = new GreetingService();
    }

    @Test
    @DisplayName("greet() formats the name into the greeting template")
    void greetFormatsName() {
        Greeting greeting = service.greet("Chien");

        assertThat(greeting.content()).isEqualTo("Hello, Chien!");
        assertThat(greeting.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("greet() trims surrounding whitespace")
    void greetTrimsWhitespace() {
        Greeting greeting = service.greet("  Ada  ");

        assertThat(greeting.content()).isEqualTo("Hello, Ada!");
    }

    @Test
    @DisplayName("id increments on each successive call")
    void idIncrements() {
        assertThat(service.greet("a").id()).isEqualTo(1L);
        assertThat(service.greet("b").id()).isEqualTo(2L);
        assertThat(service.greet("c").id()).isEqualTo(3L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("greet() rejects blank names")
    void greetRejectsBlankNames(String blank) {
        assertThatThrownBy(() -> service.greet(blank))
                .isInstanceOf(InvalidNameException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    @DisplayName("greet() rejects a null name")
    void greetRejectsNull() {
        assertThatThrownBy(() -> service.greet(null))
                .isInstanceOf(InvalidNameException.class);
    }
}
