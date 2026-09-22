package com.keyloop.web;

import com.keyloop.dto.AppointmentResponse;
import com.keyloop.dto.AvailabilityResponse;
import com.keyloop.dto.BookingRequest;
import com.keyloop.exception.NoAvailabilityException;
import com.keyloop.exception.ResourceNotFoundException;
import com.keyloop.service.AvailabilityService;
import com.keyloop.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BookingService bookingService;
    @MockitoBean private AvailabilityService availabilityService;

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(10).withNano(0);

    private AppointmentResponse sampleResponse() {
        return new AppointmentResponse(
                100L, "CONFIRMED",
                new AppointmentResponse.Ref(1L, "Downtown"),
                new AppointmentResponse.Ref(1L, "Alice"),
                new AppointmentResponse.VehicleRef(1L, "VIN123", "Toyota", "Corolla"),
                new AppointmentResponse.Ref(1L, "Carlos"),
                new AppointmentResponse.Ref(1L, "Bay A"),
                new AppointmentResponse.ServiceTypeRef(1L, "OIL_CHANGE", "Oil Change", 30),
                FUTURE, FUTURE.plusMinutes(30), Instant.now());
    }

    @Test
    void bookReturns201WithLocation() throws Exception {
        when(bookingService.book(any(BookingRequest.class))).thenReturn(sampleResponse());
        BookingRequest req = new BookingRequest(1L, 1L, 1L, FUTURE);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/appointments/100"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.technician.name").value("Carlos"))
                .andExpect(jsonPath("$.serviceBay.name").value("Bay A"));
    }

    @Test
    void bookRejectsInvalidPayloadWith400() throws Exception {
        // Missing ids and a past desiredStart -> validation failures.
        String badJson = """
                {"desiredStart":"2000-01-01T09:00:00"}
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.dealershipId").exists())
                .andExpect(jsonPath("$.fieldErrors.desiredStart").exists());
    }

    @Test
    void bookReturns409WhenNoAvailability() throws Exception {
        when(bookingService.book(any(BookingRequest.class)))
                .thenThrow(new NoAvailabilityException("No service bay available"));
        BookingRequest req = new BookingRequest(1L, 1L, 1L, FUTURE);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("No service bay available"));
    }

    @Test
    void availabilityReturnsProbeResult() throws Exception {
        when(availabilityService.check(any(), any(), any()))
                .thenReturn(AvailabilityResponse.of(FUTURE, FUTURE.plusMinutes(30), 2, 1, null));

        mockMvc.perform(get("/api/v1/appointments/availability")
                        .param("dealershipId", "1")
                        .param("serviceTypeId", "1")
                        .param("desiredStart", FUTURE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.availableTechnicians").value(2));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(bookingService.get(any())).thenThrow(new ResourceNotFoundException("Appointment", 999L));

        mockMvc.perform(get("/api/v1/appointments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found: 999"));
    }
}
