package com.zodiakk.careerservice.development.repository.specification.developmentPlan;

import com.zodiakk.careerservice.development.dto.request.filter.DevelopmentPlanFilterDto;
import com.zodiakk.careerservice.development.entity.DevelopmentPlan;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class DevelopmentPlanSpecification {

    public static Specification<DevelopmentPlan> filter(DevelopmentPlanFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getCareerGoalId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("careerGoalId"), filter.getCareerGoalId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getStartDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDateFrom()));
            }

            if (filter.getStartDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), filter.getStartDateTo()));
            }

            if (filter.getTargetDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("targetDate"), filter.getTargetDateFrom()));
            }

            if (filter.getTargetDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("targetDate"), filter.getTargetDateTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
