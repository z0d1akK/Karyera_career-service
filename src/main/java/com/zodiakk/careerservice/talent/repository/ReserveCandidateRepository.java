package com.zodiakk.careerservice.talent.repository;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.talent.entity.ReserveCandidate;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ReserveCandidateRepository extends JpaRepository<ReserveCandidate, UUID>, JpaSpecificationExecutor<ReserveCandidate> {

    List<ReserveCandidate> findAllByReserveId(UUID reserveId);

    List<ReserveCandidate> findAllByReserveIdAndStatus(UUID reserveId, ReserveCandidateStatus status);

    List<ReserveCandidate> findAllByEmployeeId(UUID employeeId);

    Optional<ReserveCandidate> findFirstByReserveIdAndEmployeeIdAndStatusIn(
            UUID reserveId,
            UUID employeeId,
            Collection<ReserveCandidateStatus> statuses
    );

    boolean existsByReserveIdAndEmployeeIdAndStatusIn(
            UUID reserveId,
            UUID employeeId,
            Collection<ReserveCandidateStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ReserveCandidate c WHERE c.id = :candidateId")
    Optional<ReserveCandidate> findByIdForUpdate(@Param("candidateId") UUID candidateId);

}
