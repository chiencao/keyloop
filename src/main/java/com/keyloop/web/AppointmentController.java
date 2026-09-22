package com.keyloop.web;

import com.keyloop.dto.AppointmentResponse;
import com.keyloop.dto.AvailabilityResponse;
import com.keyloop.dto.BookingRequest;
import com.keyloop.service.AvailabilityService;
import com.keyloop.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Book and query service appointments")
public class AppointmentController {

    private final BookingService bookingService;
    private final AvailabilityService availabilityService;

    public AppointmentController(BookingService bookingService, AvailabilityService availabilityService) {
        this.bookingService = bookingService;
        this.availabilityService = availabilityService;
    }

    @Operation(summary = "Book a service appointment",
            description = "Reserves a qualified technician and a service bay for the full service duration.")
    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookingRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        AppointmentResponse response = bookingService.book(request);
        URI location = uriBuilder.path("/api/v1/appointments/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Check availability",
            description = "Non-binding probe for a proposed window; does not reserve resources.")
    @GetMapping("/availability")
    public AvailabilityResponse availability(
            @RequestParam Long dealershipId,
            @RequestParam Long serviceTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desiredStart) {
        return availabilityService.check(dealershipId, serviceTypeId, desiredStart);
    }

    @Operation(summary = "Get an appointment by id")
    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return bookingService.get(id);
    }
}
