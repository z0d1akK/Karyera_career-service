package com.zodiakk.careerservice.talent.repository.specification.reserveDecision;

import com.zodiakk.careerservice.talent.dto.request.filter.ReserveDecisionFilterDto;
import com.zodiakk.careerservice.talent.entity.ReserveDecision;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ReserveDecisionSpecification {

    public static Specification<ReserveDecision> filter(ReserveDecisionFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getCandidateId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("candidateId"), filter.getCandidateId()));
            }

            if (filter.getDecision() != null) {
                predicates.add(criteriaBuilder.equal(root.get("decision"), filter.getDecision()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
