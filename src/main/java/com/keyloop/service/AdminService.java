package com.keyloop.service;

import com.keyloop.domain.Appointment;
import com.keyloop.domain.AppointmentStatus;
import com.keyloop.dto.AppointmentResponse;
import com.keyloop.exception.BusinessRuleException;
import com.keyloop.exception.ResourceNotFoundException;
import com.keyloop.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Back-office operations for a dealership's service desk. Completing (technician
 * sign-off) or cancelling an appointment moves it out of CONFIRMED, which — since
 * availability only counts CONFIRMED appointments — releases the technician and
 * bay for that window.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final AppointmentRepository appointmentRepository;

    public AdminService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(Long dealershipId, AppointmentStatus status) {
        return appointmentRepository.findForAdmin(dealershipId).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .map(AppointmentResponse::from)
                .toList();
    }

    /** Technician sign-off → COMPLETED, releasing the slot. */
    @Transactional
    public AppointmentResponse complete(Long id) {
        Appointment a = requireConfirmed(id, "signed off");
        a.complete();
        log.info("Appointment id={} signed off (COMPLETED) — technician={} bay={} released for {}–{}",
                a.getId(), a.getTechnician().getId(), a.getServiceBay().getId(), a.getStartTime(), a.getEndTime());
        return AppointmentResponse.from(a);
    }

    /** Cancel a booking → CANCELLED, also releasing the slot. */
    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment a = requireConfirmed(id, "cancelled");
        a.cancel();
        log.info("Appointment id={} cancelled — technician={} bay={} released", a.getId(),
                a.getTechnician().getId(), a.getServiceBay().getId());
        return AppointmentResponse.from(a);
    }

    private Appointment requireConfirmed(Long id, String action) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        if (a.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "Only a CONFIRMED appointment can be %s (id %d is %s)".formatted(action, id, a.getStatus()));
        }
        return a;
    }
}
