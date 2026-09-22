package com.keyloop.repository;

import com.keyloop.domain.ServiceBay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceBayRepository extends JpaRepository<ServiceBay, Long> {

    /**
     * Service bays at a dealership with no CONFIRMED appointment overlapping
     * [start, end).
     */
    @Query("""
            select b from ServiceBay b
            where b.dealership.id = :dealershipId
              and not exists (
                select 1 from Appointment a
                where a.serviceBay = b
                  and a.status = com.keyloop.domain.AppointmentStatus.CONFIRMED
                  and a.startTime < :end
                  and a.endTime > :start
              )
            order by b.id
            """)
    List<ServiceBay> findAvailable(@Param("dealershipId") Long dealershipId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   Pageable pageable);
}
