package com.zodiakk.careerservice.talent.repository.specification.talentReserve;

import com.zodiakk.careerservice.talent.dto.request.filter.TalentReserveFilterDto;
import com.zodiakk.careerservice.talent.entity.TalentReserve;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class TalentReserveSpecification {

    public static Specification<TalentReserve> filter(TalentReserveFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.getName().trim().toLowerCase() + "%"
                ));
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
