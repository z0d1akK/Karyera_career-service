package com.zodiakk.careerservice.career.repository.specification.careerTrackRevision;

import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackRevisionFilterDto;
import com.zodiakk.careerservice.career.entity.CareerTrackRevision;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class CareerTrackRevisionSpecification {

    public static Specification<CareerTrackRevision> filter(CareerTrackRevisionFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getTrackId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("trackId"), filter.getTrackId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
