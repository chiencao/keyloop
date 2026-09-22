package com.keyloop.repository;

import com.keyloop.domain.Dealership;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DealershipRepository extends JpaRepository<Dealership, Long> {

    /**
     * Acquires a row-level write lock on the dealership so that concurrent
     * booking transactions for the same dealership are serialized. This closes
     * the check-then-act race between the availability query and the insert.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dealership d where d.id = :id")
    Optional<Dealership> findByIdForUpdate(@Param("id") Long id);
}
