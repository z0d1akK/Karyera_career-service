package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.EmployeeCareerGoal;
import com.zodiakk.careerservice.common.enums.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface EmployeeCareerGoalRepository extends JpaRepository<EmployeeCareerGoal, UUID>, JpaSpecificationExecutor<EmployeeCareerGoal> {

    List<EmployeeCareerGoal> findAllByEmployeeId(UUID employeeId);

    List<EmployeeCareerGoal> findAllByEmployeeIdAndStatus(UUID employeeId, CareerGoalStatus status);

    Optional<EmployeeCareerGoal> findFirstByEmployeeIdAndStatus(UUID employeeId, CareerGoalStatus status);

    boolean existsByEmployeeIdAndStatus(UUID employeeId, CareerGoalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM EmployeeCareerGoal g WHERE g.id = :goalId")
    Optional<EmployeeCareerGoal> findByIdForUpdate(@Param("goalId") UUID goalId);

}
