package com.zodiakk.careerservice.development.repository;

import com.zodiakk.careerservice.development.entity.PlanReview;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface PlanReviewRepository extends JpaRepository<PlanReview, UUID>, JpaSpecificationExecutor<PlanReview> {

    List<PlanReview> findAllByPlanIdOrderByReviewedAtDesc(UUID planId);

}
