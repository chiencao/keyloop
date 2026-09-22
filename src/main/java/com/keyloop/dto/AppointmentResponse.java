package com.keyloop.dto;

import com.keyloop.domain.Appointment;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Confirmed appointment view returned to clients. Associates the customer,
 * vehicle, technician and service bay as required by the scenario.
 */
public record AppointmentResponse(
        Long id,
        String status,
        Ref dealership,
        Ref customer,
        VehicleRef vehicle,
        Ref technician,
        Ref serviceBay,
        ServiceTypeRef serviceType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Instant createdAt
) {

    public record Ref(Long id, String name) {
    }

    public record VehicleRef(Long id, String vin, String make, String model) {
    }

    public record ServiceTypeRef(Long id, String code, String name, int durationMinutes) {
    }

    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getStatus().name(),
                new Ref(a.getDealership().getId(), a.getDealership().getName()),
                new Ref(a.getCustomer().getId(), a.getCustomer().getName()),
                new VehicleRef(a.getVehicle().getId(), a.getVehicle().getVin(),
                        a.getVehicle().getMake(), a.getVehicle().getModel()),
                new Ref(a.getTechnician().getId(), a.getTechnician().getName()),
                new Ref(a.getServiceBay().getId(), a.getServiceBay().getName()),
                new ServiceTypeRef(a.getServiceType().getId(), a.getServiceType().getCode(),
                        a.getServiceType().getName(), a.getServiceType().getDurationMinutes()),
                a.getStartTime(),
                a.getEndTime(),
                a.getCreatedAt()
        );
    }
}
