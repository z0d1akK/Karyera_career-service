package com.zodiakk.careerservice.career.repository.specification.employeeCareerGoal;

import com.zodiakk.careerservice.career.dto.request.filter.EmployeeCareerGoalFilterDto;
import com.zodiakk.careerservice.career.entity.EmployeeCareerGoal;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class EmployeeCareerGoalSpecification {

    public static Specification<EmployeeCareerGoal> filter(EmployeeCareerGoalFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getTargetPositionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetPositionId"), filter.getTargetPositionId()));
            }

            if (filter.getTargetGradeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetGradeId"), filter.getTargetGradeId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
