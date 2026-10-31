package com.zodiakk.careerservice.development.repository.specification.activityEvidence;

import com.zodiakk.careerservice.development.dto.request.filter.ActivityEvidenceFilterDto;
import com.zodiakk.careerservice.development.entity.ActivityEvidence;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ActivityEvidenceSpecification {

    public static Specification<ActivityEvidence> filter(ActivityEvidenceFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getActivityId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("activityId"), filter.getActivityId()));
            }

            if (filter.getVerificationStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("verificationStatus"), filter.getVerificationStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
