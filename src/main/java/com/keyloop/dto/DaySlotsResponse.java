package com.keyloop.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Availability across one business day for a dealership + service type. Powers
 * the "pick a time" step. {@code offered} is false when the dealership has no
 * technician qualified for the service.
 */
public record DaySlotsResponse(
        LocalDate date,
        Long dealershipId,
        Long serviceTypeId,
        int durationMinutes,
        boolean offered,
        List<SlotView> slots
) {
}
