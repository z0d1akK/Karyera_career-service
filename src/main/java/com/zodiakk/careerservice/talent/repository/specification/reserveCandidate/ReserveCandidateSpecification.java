package com.zodiakk.careerservice.talent.repository.specification.reserveCandidate;

import com.zodiakk.careerservice.talent.dto.request.filter.ReserveCandidateFilterDto;
import com.zodiakk.careerservice.talent.entity.ReserveCandidate;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ReserveCandidateSpecification {

    public static Specification<ReserveCandidate> filter(ReserveCandidateFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getReserveId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("reserveId"), filter.getReserveId()));
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
