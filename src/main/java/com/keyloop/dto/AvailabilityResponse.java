package com.keyloop.dto;

import java.time.LocalDateTime;

/**
 * Result of a non-binding availability probe for a proposed service window.
 */
public record AvailabilityResponse(
        boolean available,
        LocalDateTime start,
        LocalDateTime end,
        int availableTechnicians,
        int availableBays,
        String reason
) {
    public static AvailabilityResponse of(LocalDateTime start, LocalDateTime end,
                                          int technicians, int bays, String reason) {
        boolean available = technicians > 0 && bays > 0 && reason == null;
        return new AvailabilityResponse(available, start, end, technicians, bays, reason);
    }
}
