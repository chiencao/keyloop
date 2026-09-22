package com.keyloop.repository;

import com.keyloop.domain.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** All CONFIRMED appointments at a dealership intersecting [from, to) — for the day-slots view. */
    @Query("""
            select a from Appointment a
            where a.dealership.id = :dealershipId
              and a.status = com.keyloop.domain.AppointmentStatus.CONFIRMED
              and a.startTime < :to
              and a.endTime > :from
            """)
    List<Appointment> findConfirmedForDealershipInWindow(@Param("dealershipId") Long dealershipId,
                                                         @Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    /** All appointments at a dealership for the admin desk, associations fetched to avoid N+1. */
    @Query("""
            select a from Appointment a
              join fetch a.dealership
              join fetch a.customer
              join fetch a.vehicle
              join fetch a.technician
              join fetch a.serviceBay
              join fetch a.serviceType
            where a.dealership.id = :dealershipId
            order by a.startTime
            """)
    List<Appointment> findForAdmin(@Param("dealershipId") Long dealershipId);

    /**
     * True if the vehicle already has a CONFIRMED appointment overlapping
     * [start, end). Same half-open overlap predicate used for resources:
     * {@code existing.start < end AND existing.end > start}.
     */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.vehicle.id = :vehicleId
              and a.status = com.keyloop.domain.AppointmentStatus.CONFIRMED
              and a.startTime < :end
              and a.endTime > :start
            """)
    boolean existsConfirmedForVehicleOverlapping(@Param("vehicleId") Long vehicleId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /** A vehicle's CONFIRMED appointments intersecting [from, to) — any dealership — for the day-slots view. */
    @Query("""
            select a from Appointment a
            where a.vehicle.id = :vehicleId
              and a.status = com.keyloop.domain.AppointmentStatus.CONFIRMED
              and a.startTime < :to
              and a.endTime > :from
            """)
    List<Appointment> findConfirmedForVehicleInWindow(@Param("vehicleId") Long vehicleId,
                                                      @Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
}
