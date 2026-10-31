package com.zodiakk.careerservice.career.repository.specification.positionRequirement;

import com.zodiakk.careerservice.career.dto.request.filter.PositionRequirementFilterDto;
import com.zodiakk.careerservice.career.entity.PositionRequirement;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class PositionRequirementSpecification {

    public static Specification<PositionRequirement> filter(PositionRequirementFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getPositionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("positionId"), filter.getPositionId()));
            }

            if (filter.getGradeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("gradeId"), filter.getGradeId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
