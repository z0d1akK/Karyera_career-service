package com.zodiakk.careerservice.career.repository.specification.careerTransition;

import com.zodiakk.careerservice.career.dto.request.filter.CareerTransitionFilterDto;
import com.zodiakk.careerservice.career.entity.CareerTransition;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class CareerTransitionSpecification {

    public static Specification<CareerTransition> filter(CareerTransitionFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getSourceNodeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceNodeId"), filter.getSourceNodeId()));
            }

            if (filter.getTargetNodeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetNodeId"), filter.getTargetNodeId()));
            }

            if (filter.getActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), filter.getActive()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
