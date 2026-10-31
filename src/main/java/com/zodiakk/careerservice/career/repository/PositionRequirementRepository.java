package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.PositionRequirement;
import com.zodiakk.careerservice.common.enums.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PositionRequirementRepository extends JpaRepository<PositionRequirement, UUID>, JpaSpecificationExecutor<PositionRequirement> {

    List<PositionRequirement> findAllByPositionIdAndGradeIdOrderByRevisionDesc(UUID positionId, UUID gradeId);

    Optional<PositionRequirement> findTopByPositionIdAndGradeIdOrderByRevisionDesc(UUID positionId, UUID gradeId);

    Optional<PositionRequirement> findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
            UUID positionId,
            UUID gradeId,
            RequirementStatus status
    );

    boolean existsByPositionIdAndGradeIdAndRevision(UUID positionId, UUID gradeId, Integer revision);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PositionRequirement r WHERE r.id = :requirementId")
    Optional<PositionRequirement> findByIdForUpdate(@Param("requirementId") UUID requirementId);

}
