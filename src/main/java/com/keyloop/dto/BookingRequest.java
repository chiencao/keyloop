package com.keyloop.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Client request to book a service appointment. {@code desiredStart} is
 * dealership-local wall-clock time (ISO-8601, e.g. 2026-10-01T09:00:00).
 */
public record BookingRequest(

        @NotNull(message = "dealershipId is required")
        Long dealershipId,

        @NotNull(message = "vehicleId is required")
        Long vehicleId,

        @NotNull(message = "serviceTypeId is required")
        Long serviceTypeId,

        @NotNull(message = "desiredStart is required")
        @Future(message = "desiredStart must be in the future")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime desiredStart
) {
}
