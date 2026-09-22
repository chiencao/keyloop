package com.keyloop.service;

import com.keyloop.domain.Appointment;
import com.keyloop.domain.Dealership;
import com.keyloop.domain.ServiceBay;
import com.keyloop.domain.ServiceType;
import com.keyloop.domain.Technician;
import com.keyloop.domain.Vehicle;
import com.keyloop.dto.AppointmentResponse;
import com.keyloop.dto.BookingRequest;
import com.keyloop.exception.BusinessRuleException;
import com.keyloop.exception.DuplicateBookingException;
import com.keyloop.exception.NoAvailabilityException;
import com.keyloop.exception.ResourceNotFoundException;
import com.keyloop.repository.AppointmentRepository;
import com.keyloop.repository.DealershipRepository;
import com.keyloop.repository.ServiceBayRepository;
import com.keyloop.repository.ServiceTypeRepository;
import com.keyloop.repository.TechnicianRepository;
import com.keyloop.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Owns the resource-constrained booking use case. The whole method runs in one
 * transaction that first takes a pessimistic write lock on the dealership,
 * serializing concurrent bookings for that dealership so the availability check
 * and the insert are atomic (no double-booking).
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final PageRequest FIRST = PageRequest.of(0, 1);

    private final DealershipRepository dealershipRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final TechnicianRepository technicianRepository;
    private final ServiceBayRepository serviceBayRepository;
    private final AppointmentRepository appointmentRepository;

    public BookingService(DealershipRepository dealershipRepository,
                          VehicleRepository vehicleRepository,
                          ServiceTypeRepository serviceTypeRepository,
                          TechnicianRepository technicianRepository,
                          ServiceBayRepository serviceBayRepository,
                          AppointmentRepository appointmentRepository) {
        this.dealershipRepository = dealershipRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.technicianRepository = technicianRepository;
        this.serviceBayRepository = serviceBayRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public AppointmentResponse book(BookingRequest request) {
        ServiceType serviceType = serviceTypeRepository.findById(request.serviceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("ServiceType", request.serviceTypeId()));

        // Lock order: vehicle first, then dealership. Both locks are held for the
        // whole transaction; a consistent order avoids deadlocks between the
        // same-vehicle guard and the per-dealership resource serialization.
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", request.vehicleId()));

        // Serialize bookings for this dealership: closes the check-then-act race.
        Dealership dealership = dealershipRepository.findByIdForUpdate(request.dealershipId())
                .orElseThrow(() -> new ResourceNotFoundException("Dealership", request.dealershipId()));

        LocalDateTime start = request.desiredStart();
        LocalDateTime end = start.plus(serviceType.getDuration());

        String hoursViolation = AvailabilityService.validateBusinessHours(dealership, start, end);
        if (hoursViolation != null) {
            throw new BusinessRuleException(hoursViolation);
        }

        // A vehicle can only be serviced in one place at a time.
        if (appointmentRepository.existsConfirmedForVehicleOverlapping(vehicle.getId(), start, end)) {
            throw new DuplicateBookingException(
                    "Vehicle %d already has an appointment during %s–%s"
                            .formatted(vehicle.getId(), start, end));
        }

        List<Technician> technicians = technicianRepository.findAvailable(
                dealership.getId(), serviceType.getRequiredSkill(), start, end, FIRST);
        if (technicians.isEmpty()) {
            throw new NoAvailabilityException(
                    "No qualified technician (skill '%s') available for %s–%s"
                            .formatted(serviceType.getRequiredSkill(), start, end));
        }

        List<ServiceBay> bays = serviceBayRepository.findAvailable(dealership.getId(), start, end, FIRST);
        if (bays.isEmpty()) {
            throw new NoAvailabilityException("No service bay available for %s–%s".formatted(start, end));
        }

        Technician technician = technicians.get(0);
        ServiceBay bay = bays.get(0);

        Appointment appointment = new Appointment(
                dealership, vehicle.getCustomer(), vehicle, technician, bay, serviceType, start, end);
        Appointment saved = appointmentRepository.save(appointment);

        log.info("Booked appointment id={} dealership={} serviceType={} technician={} bay={} window={}–{}",
                saved.getId(), dealership.getId(), serviceType.getCode(),
                technician.getId(), bay.getId(), start, end);

        return AppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        return AppointmentResponse.from(appointment);
    }
}
