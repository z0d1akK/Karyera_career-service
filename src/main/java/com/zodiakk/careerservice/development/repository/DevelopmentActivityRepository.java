package com.zodiakk.careerservice.development.repository;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.development.entity.DevelopmentActivity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface DevelopmentActivityRepository extends JpaRepository<DevelopmentActivity, UUID>, JpaSpecificationExecutor<DevelopmentActivity> {

    List<DevelopmentActivity> findAllByPlanIdOrderByDueDateAsc(UUID planId);

    List<DevelopmentActivity> findAllByPlanIdAndStatus(UUID planId, DevelopmentActivityStatus status);

    List<DevelopmentActivity> findAllBySkillId(UUID skillId);

    long countByPlanId(UUID planId);

    long countByPlanIdAndStatus(UUID planId, DevelopmentActivityStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM DevelopmentActivity a WHERE a.id = :activityId")
    Optional<DevelopmentActivity> findByIdForUpdate(@Param("activityId") UUID activityId);

}
