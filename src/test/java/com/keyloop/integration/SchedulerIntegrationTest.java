package com.keyloop.integration;

import com.keyloop.domain.AppointmentStatus;
import com.keyloop.dto.AppointmentResponse;
import com.keyloop.dto.BookingRequest;
import com.keyloop.exception.DuplicateBookingException;
import com.keyloop.exception.NoAvailabilityException;
import com.keyloop.repository.AppointmentRepository;
import com.keyloop.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests against the real Spring context, Flyway-migrated schema and
 * seeded H2 database. Seed data (see V2 migration): dealership 1 has 3
 * GENERAL-capable technicians but only 2 service bays, so an OIL_CHANGE slot is
 * capacity-limited to 2 concurrent bookings.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SchedulerIntegrationTest {

    private static final long DEALERSHIP = 1L;
    private static final long VEHICLE = 1L;
    private static final long OIL_CHANGE = 1L;
    private static final LocalDateTime SLOT = LocalDateTime.of(2030, 6, 1, 9, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookingService bookingService;
    @Autowired private AppointmentRepository appointmentRepository;

    @BeforeEach
    void cleanAppointments() {
        appointmentRepository.deleteAllInBatch();
    }

    @Test
    void booksAndPersistsAppointmentThroughRestApi() throws Exception {
        BookingRequest req = new BookingRequest(DEALERSHIP, VEHICLE, OIL_CHANGE, SLOT);

        String body = mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.customer.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.technician.id").exists())
                .andExpect(jsonPath("$.serviceBay.id").exists())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/v1/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.endTime").value("2030-06-01T09:30:00"));

        assertThat(appointmentRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsBookingBeyondBayCapacity() {
        // 3 technicians hold GENERAL but only 2 bays exist: 3rd booking must fail.
        // Distinct vehicles so the same-vehicle guard doesn't mask the bay limit.
        AppointmentResponse first = bookingService.book(new BookingRequest(DEALERSHIP, 1L, OIL_CHANGE, SLOT));
        AppointmentResponse second = bookingService.book(new BookingRequest(DEALERSHIP, 2L, OIL_CHANGE, SLOT));

        assertThat(first.serviceBay().id()).isNotEqualTo(second.serviceBay().id());
        assertThat(first.technician().id()).isNotEqualTo(second.technician().id());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> bookingService.book(new BookingRequest(DEALERSHIP, 3L, OIL_CHANGE, SLOT)))
                .isInstanceOf(NoAvailabilityException.class);

        assertThat(appointmentRepository.count()).isEqualTo(2);
    }

    @Test
    void rejectsSameVehicleDoubleBookingButAllowsOtherVehicles() {
        bookingService.book(new BookingRequest(DEALERSHIP, 1L, OIL_CHANGE, SLOT)); // 09:00–09:30

        // Same vehicle, overlapping window -> duplicate booking rejected.
        BookingRequest overlap = new BookingRequest(DEALERSHIP, 1L, OIL_CHANGE, SLOT.plusMinutes(15));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> bookingService.book(overlap))
                .isInstanceOf(DuplicateBookingException.class)
                .hasMessageContaining("already has an appointment");

        // A different vehicle in the same overlapping window is still fine.
        AppointmentResponse other = bookingService.book(
                new BookingRequest(DEALERSHIP, 2L, OIL_CHANGE, SLOT.plusMinutes(15)));
        assertThat(other.status()).isEqualTo("CONFIRMED");

        assertThat(appointmentRepository.count()).isEqualTo(2);
    }

    @Test
    void vehiclesAreScopedToTheSelectedCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // Customer 1 (Alice) owns vehicles 1, 3, 9 — and only those come back.
        mockMvc.perform(get("/api/v1/vehicles").param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].customerName", everyItem(is("Alice Johnson"))));
    }

    @Test
    void daySlotsReportsOfferedAndAvailability() throws Exception {
        String date = LocalDate.of(2030, 6, 3).toString();

        // Dealership 1 offers Oil Change (GENERAL) — slots present and some available.
        mockMvc.perform(get("/api/v1/appointments/slots")
                        .param("dealershipId", "1").param("serviceTypeId", "1").param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offered").value(true))
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.slots[0].available").value(true));

        // Dealership 3 (Riverside) has no EV-certified technician -> not offered.
        mockMvc.perform(get("/api/v1/appointments/slots")
                        .param("dealershipId", "3").param("serviceTypeId", "4").param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offered").value(false));
    }

    @Test
    void concurrentBookingsForSameSlotNeverOverbook() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            long vehicleId = i + 1;   // distinct vehicle per thread (seeded 1..10)
            tasks.add(() -> {
                startGate.await();
                try {
                    bookingService.book(new BookingRequest(DEALERSHIP, vehicleId, OIL_CHANGE, SLOT));
                    success.incrementAndGet();
                } catch (NoAvailabilityException expected) {
                    rejected.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        startGate.countDown();               // release all threads at once
        for (Future<Void> f : futures) {
            f.get();                          // propagate unexpected exceptions
        }
        pool.shutdown();

        // Capacity is 2 (bays); the pessimistic lock must prevent overbooking.
        assertThat(success.get()).isEqualTo(2);
        assertThat(rejected.get()).isEqualTo(threads - 2);
        assertThat(appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .count()).isEqualTo(2);
    }
}
