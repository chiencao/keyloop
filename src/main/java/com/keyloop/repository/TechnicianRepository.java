package com.keyloop.repository;

import com.keyloop.domain.Technician;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    /**
     * Technicians at a dealership who hold the required skill and have no
     * CONFIRMED appointment overlapping [start, end). Two intervals overlap
     * when {@code existing.start < end AND existing.end > start}.
     */
    @Query("""
            select t from Technician t
            where t.dealership.id = :dealershipId
              and :requiredSkill member of t.skills
              and not exists (
                select 1 from Appointment a
                where a.technician = t
                  and a.status = com.keyloop.domain.AppointmentStatus.CONFIRMED
                  and a.startTime < :end
                  and a.endTime > :start
              )
            order by t.id
            """)
    List<Technician> findAvailable(@Param("dealershipId") Long dealershipId,
                                   @Param("requiredSkill") String requiredSkill,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   Pageable pageable);

    /** Technicians of a dealership with their skills eagerly loaded (for the day-slots view). */
    @Query("select distinct t from Technician t left join fetch t.skills where t.dealership.id = :dealershipId")
    List<Technician> findByDealershipWithSkills(@Param("dealershipId") Long dealershipId);
}
