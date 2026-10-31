package com.zodiakk.careerservice.talent.repository;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.talent.entity.TalentReserve;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.*;

@Repository
public interface TalentReserveRepository extends JpaRepository<TalentReserve, UUID>, JpaSpecificationExecutor<TalentReserve> {

    List<TalentReserve> findAllByStatus(TalentReserveStatus status);

    List<TalentReserve> findAllByTargetPositionIdAndTargetGradeIdAndStatus(
            UUID targetPositionId,
            UUID targetGradeId,
            TalentReserveStatus status
    );

    List<TalentReserve> findAllByEffectiveToIsNullOrEffectiveToGreaterThanEqual(LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM TalentReserve r WHERE r.id = :reserveId")
    Optional<TalentReserve> findByIdForUpdate(@Param("reserveId") UUID reserveId);

}
