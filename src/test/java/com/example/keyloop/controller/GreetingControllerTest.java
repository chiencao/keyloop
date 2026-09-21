package com.example.keyloop.controller;

import com.example.keyloop.exception.InvalidNameException;
import com.example.keyloop.model.Greeting;
import com.example.keyloop.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test. Only the MVC infrastructure is loaded; the service is
 * replaced by a Mockito mock so we test request mapping, serialization and
 * error handling in isolation.
 */
@WebMvcTest(GreetingController.class)
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GreetingService greetingService;

    @Test
    void returnsGreetingJson() throws Exception {
        when(greetingService.greet(eq("Chien"))).thenReturn(new Greeting(1L, "Hello, Chien!"));

        mockMvc.perform(get("/greeting").param("name", "Chien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("Hello, Chien!"));
    }

    @Test
    void usesDefaultNameWhenParamMissing() throws Exception {
        when(greetingService.greet(eq("World"))).thenReturn(new Greeting(1L, "Hello, World!"));

        mockMvc.perform(get("/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello, World!"));
    }

    @Test
    void returnsBadRequestOnInvalidName() throws Exception {
        when(greetingService.greet(eq("bad")))
                .thenThrow(new InvalidNameException("name must not be blank"));

        mockMvc.perform(get("/greeting").param("name", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("name must not be blank"));
    }
}
