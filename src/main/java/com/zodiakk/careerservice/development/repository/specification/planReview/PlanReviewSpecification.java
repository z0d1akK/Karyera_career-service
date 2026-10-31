package com.zodiakk.careerservice.development.repository.specification.planReview;

import com.zodiakk.careerservice.development.dto.request.filter.PlanReviewFilterDto;
import com.zodiakk.careerservice.development.entity.PlanReview;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class PlanReviewSpecification {

    public static Specification<PlanReview> filter(PlanReviewFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getPlanId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("planId"), filter.getPlanId()));
            }

            if (filter.getReviewerEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("reviewerEmployeeId"), filter.getReviewerEmployeeId()));
            }

            if (filter.getDecision() != null) {
                predicates.add(criteriaBuilder.equal(root.get("decision"), filter.getDecision()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
