package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.CareerTransition;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CareerTransitionRepository extends JpaRepository<CareerTransition, UUID>, JpaSpecificationExecutor<CareerTransition> {

    List<CareerTransition> findAllBySourceNodeId(UUID sourceNodeId);

    List<CareerTransition> findAllBySourceNodeIdIn(Collection<UUID> sourceNodeIds);

    List<CareerTransition> findAllByTargetNodeId(UUID targetNodeId);

    List<CareerTransition> findAllBySourceNodeIdAndActiveTrue(UUID sourceNodeId);

    boolean existsBySourceNodeIdAndTargetNodeId(UUID sourceNodeId, UUID targetNodeId);

}
