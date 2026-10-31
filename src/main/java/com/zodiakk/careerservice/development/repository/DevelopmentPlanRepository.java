package com.zodiakk.careerservice.development.repository;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.development.entity.DevelopmentPlan;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface DevelopmentPlanRepository extends JpaRepository<DevelopmentPlan, UUID>, JpaSpecificationExecutor<DevelopmentPlan> {

    List<DevelopmentPlan> findAllByEmployeeId(UUID employeeId);

    List<DevelopmentPlan> findAllByEmployeeIdAndStatus(UUID employeeId, DevelopmentPlanStatus status);

    List<DevelopmentPlan> findAllByCareerGoalId(UUID careerGoalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM DevelopmentPlan p WHERE p.id = :planId")
    Optional<DevelopmentPlan> findByIdForUpdate(@Param("planId") UUID planId);

}
