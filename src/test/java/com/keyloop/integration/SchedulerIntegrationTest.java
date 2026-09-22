package com.keyloop.integration;

import com.keyloop.domain.AppointmentStatus;
import com.keyloop.dto.AppointmentResponse;
import com.keyloop.dto.BookingRequest;
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
        BookingRequest req = new BookingRequest(DEALERSHIP, VEHICLE, OIL_CHANGE, SLOT);

        AppointmentResponse first = bookingService.book(req);
        AppointmentResponse second = bookingService.book(req);

        assertThat(first.serviceBay().id()).isNotEqualTo(second.serviceBay().id());
        assertThat(first.technician().id()).isNotEqualTo(second.technician().id());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> bookingService.book(req))
                .isInstanceOf(NoAvailabilityException.class);

        assertThat(appointmentRepository.count()).isEqualTo(2);
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
            tasks.add(() -> {
                startGate.await();
                try {
                    bookingService.book(new BookingRequest(DEALERSHIP, VEHICLE, OIL_CHANGE, SLOT));
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
