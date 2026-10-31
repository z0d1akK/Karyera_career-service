package com.zodiakk.careerservice.career.repository.specification.requiredSkill;

import com.zodiakk.careerservice.career.dto.request.filter.RequiredSkillFilterDto;
import com.zodiakk.careerservice.career.entity.RequiredSkill;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class RequiredSkillSpecification {

    public static Specification<RequiredSkill> filter(RequiredSkillFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getRequirementId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("requirementId"), filter.getRequirementId()));
            }

            if (filter.getSkillId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("skillId"), filter.getSkillId()));
            }

            if (filter.getMandatory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("mandatory"), filter.getMandatory()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
