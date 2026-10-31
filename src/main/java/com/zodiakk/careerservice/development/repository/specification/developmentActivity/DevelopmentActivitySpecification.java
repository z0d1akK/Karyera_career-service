package com.zodiakk.careerservice.development.repository.specification.developmentActivity;

import com.zodiakk.careerservice.development.dto.request.filter.DevelopmentActivityFilterDto;
import com.zodiakk.careerservice.development.entity.DevelopmentActivity;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class DevelopmentActivitySpecification {

    public static Specification<DevelopmentActivity> filter(DevelopmentActivityFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getPlanId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("planId"), filter.getPlanId()));
            }

            if (filter.getSkillId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("skillId"), filter.getSkillId()));
            }

            if (filter.getActivityType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("activityType"), filter.getActivityType()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getDueDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }

            if (filter.getDueDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
