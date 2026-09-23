package com.keyloop.service;

import com.keyloop.domain.Appointment;
import com.keyloop.domain.Dealership;
import com.keyloop.domain.ServiceBay;
import com.keyloop.domain.ServiceType;
import com.keyloop.domain.Technician;
import com.keyloop.dto.AvailabilityResponse;
import com.keyloop.dto.DaySlotsResponse;
import com.keyloop.dto.SlotView;
import com.keyloop.dto.TechnicianOption;
import com.keyloop.exception.ResourceNotFoundException;
import com.keyloop.repository.AppointmentRepository;
import com.keyloop.repository.DealershipRepository;
import com.keyloop.repository.ServiceBayRepository;
import com.keyloop.repository.ServiceTypeRepository;
import com.keyloop.repository.TechnicianRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final AppointmentRepository appointmentRepository;

    public AvailabilityService(DealershipRepository dealershipRepository,
                               ServiceTypeRepository serviceTypeRepository,
                               TechnicianRepository technicianRepository,
                               ServiceBayRepository serviceBayRepository,
                               AppointmentRepository appointmentRepository) {
        this.dealershipRepository = dealershipRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.technicianRepository = technicianRepository;
        this.serviceBayRepository = serviceBayRepository;
        this.appointmentRepository = appointmentRepository;
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

    /** Qualified technicians free for a specific proposed window — so the customer can pick one. */
    @Transactional(readOnly = true)
    public List<TechnicianOption> availableTechnicians(Long dealershipId, Long serviceTypeId, LocalDateTime desiredStart) {
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", serviceTypeId));
        LocalDateTime end = desiredStart.plus(serviceType.getDuration());
        return technicianRepository
                .findAvailable(dealershipId, serviceType.getRequiredSkill(), desiredStart, end, PageRequest.of(0, 100))
                .stream()
                .map(t -> new TechnicianOption(t.getId(), t.getName()))
                .toList();
    }

    /**
     * Availability for every 30-minute start across one business day, computed
     * in memory from a single appointments query plus the dealership's
     * technicians and bays. A slot is bookable when a skilled technician and a
     * bay are both free for the full duration and the start is still in the future.
     */
    @Transactional(readOnly = true)
    public DaySlotsResponse slots(Long dealershipId, Long serviceTypeId, LocalDate date, Long vehicleId) {
        Dealership dealership = dealershipRepository.findById(dealershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealership", dealershipId));
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", serviceTypeId));

        int duration = serviceType.getDurationMinutes();
        String skill = serviceType.getRequiredSkill();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Appointment> appts = appointmentRepository
                .findConfirmedForDealershipInWindow(dealershipId, dayStart, dayEnd);

        // The queried vehicle's own bookings (any dealership) — to flag slots it already holds.
        List<Appointment> vehicleAppts = vehicleId == null ? List.of()
                : appointmentRepository.findConfirmedForVehicleInWindow(vehicleId, dayStart, dayEnd);

        List<Technician> skilledTechs = technicianRepository.findByDealershipWithSkills(dealershipId).stream()
                .filter(t -> t.getSkills().contains(skill))
                .toList();
        List<ServiceBay> bays = serviceBayRepository.findByDealershipIdOrderById(dealershipId);
        boolean offered = !skilledTechs.isEmpty();

        int openMin = dealership.getOpeningTime().toSecondOfDay() / 60;
        int closeMin = dealership.getClosingTime().toSecondOfDay() / 60;
        LocalDateTime now = LocalDateTime.now();

        List<SlotView> slots = new ArrayList<>();
        for (int m = openMin; m + duration <= closeMin; m += 30) {
            LocalDateTime start = date.atTime(m / 60, m % 60);
            LocalDateTime end = start.plusMinutes(duration);

            List<Appointment> overlap = appts.stream()
                    .filter(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(start))
                    .toList();
            Set<Long> busyTechIds = overlap.stream().map(a -> a.getTechnician().getId()).collect(Collectors.toSet());
            Set<Long> busyBayIds = overlap.stream().map(a -> a.getServiceBay().getId()).collect(Collectors.toSet());

            int freeTechs = (int) skilledTechs.stream().filter(t -> !busyTechIds.contains(t.getId())).count();
            int freeBays = (int) bays.stream().filter(b -> !busyBayIds.contains(b.getId())).count();
            boolean available = offered && freeTechs > 0 && freeBays > 0 && start.isAfter(now);
            boolean mineBooked = vehicleAppts.stream()
                    .anyMatch(a -> a.getStartTime().isBefore(end) && a.getEndTime().isAfter(start));

            slots.add(new SlotView("%02d:%02d".formatted(m / 60, m % 60), available, freeTechs, freeBays, mineBooked));
        }
        return new DaySlotsResponse(date, dealershipId, serviceTypeId, duration, offered, slots);
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
