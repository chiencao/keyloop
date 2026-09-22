package com.keyloop.service;

import com.keyloop.domain.Dealership;
import com.keyloop.domain.ServiceType;
import com.keyloop.dto.AvailabilityResponse;
import com.keyloop.exception.ResourceNotFoundException;
import com.keyloop.repository.DealershipRepository;
import com.keyloop.repository.ServiceBayRepository;
import com.keyloop.repository.ServiceTypeRepository;
import com.keyloop.repository.TechnicianRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Read-only, non-binding availability probe. Reports whether a qualified
 * technician and a service bay both exist for the requested window. It does not
 * reserve anything — {@link BookingService} performs the binding, race-safe check.
 */
@Service
public class AvailabilityService {

    private final DealershipRepository dealershipRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TechnicianRepository technicianRepository;
    private final ServiceBayRepository serviceBayRepository;

    public AvailabilityService(DealershipRepository dealershipRepository,
                               ServiceTypeRepository serviceTypeRepository,
                               TechnicianRepository technicianRepository,
                               ServiceBayRepository serviceBayRepository) {
        this.dealershipRepository = dealershipRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.technicianRepository = technicianRepository;
        this.serviceBayRepository = serviceBayRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse check(Long dealershipId, Long serviceTypeId, LocalDateTime desiredStart) {
        Dealership dealership = dealershipRepository.findById(dealershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealership", dealershipId));
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", serviceTypeId));

        LocalDateTime end = desiredStart.plus(serviceType.getDuration());

        String hoursViolation = validateBusinessHours(dealership, desiredStart, end);
        if (hoursViolation != null) {
            return AvailabilityResponse.of(desiredStart, end, 0, 0, hoursViolation);
        }

        int technicians = technicianRepository
                .findAvailable(dealershipId, serviceType.getRequiredSkill(), desiredStart, end, PageRequest.of(0, 50))
                .size();
        int bays = serviceBayRepository
                .findAvailable(dealershipId, desiredStart, end, PageRequest.of(0, 50))
                .size();

        return AvailabilityResponse.of(desiredStart, end, technicians, bays, null);
    }

    /**
     * Returns a human-readable reason if the window falls outside opening hours,
     * or {@code null} if it is acceptable. Shared rule used by the booking path.
     */
    static String validateBusinessHours(Dealership dealership, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return "Service window may not span multiple days";
        }
        LocalTime opening = dealership.getOpeningTime();
        LocalTime closing = dealership.getClosingTime();
        if (start.toLocalTime().isBefore(opening) || end.toLocalTime().isAfter(closing)) {
            return "Requested window %s–%s is outside opening hours %s–%s"
                    .formatted(start.toLocalTime(), end.toLocalTime(), opening, closing);
        }
        return null;
    }
}
