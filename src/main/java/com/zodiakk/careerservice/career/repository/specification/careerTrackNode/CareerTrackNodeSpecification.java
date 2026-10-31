package com.zodiakk.careerservice.career.repository.specification.careerTrackNode;

import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackNodeFilterDto;
import com.zodiakk.careerservice.career.entity.CareerTrackNode;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class CareerTrackNodeSpecification {

    public static Specification<CareerTrackNode> filter(CareerTrackNodeFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getTrackId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("trackId"), filter.getTrackId()));
            }

            if (filter.getPositionId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("positionId"), filter.getPositionId()));
            }

            if (filter.getGradeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("gradeId"), filter.getGradeId()));
            }

            if (filter.getIsEntryPoint() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isEntryPoint"), filter.getIsEntryPoint()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
