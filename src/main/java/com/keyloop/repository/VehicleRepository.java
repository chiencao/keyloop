package com.keyloop.repository;

import com.keyloop.domain.Vehicle;
import com.keyloop.dto.VehicleView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Locks the vehicle row so concurrent bookings for the same vehicle are
     * serialized (even across different dealerships), making the same-vehicle
     * double-booking guard race-safe. Acquired before the dealership lock to keep
     * a consistent lock order and avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select new com.keyloop.dto.VehicleView(v.id, v.vin, v.make, v.model, c.name)
            from Vehicle v join v.customer c
            order by v.id
            """)
    List<VehicleView> listAll();
}
