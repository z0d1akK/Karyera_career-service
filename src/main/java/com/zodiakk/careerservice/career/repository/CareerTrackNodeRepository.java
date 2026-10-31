package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.CareerTrackNode;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CareerTrackNodeRepository extends JpaRepository<CareerTrackNode, UUID>, JpaSpecificationExecutor<CareerTrackNode> {

    List<CareerTrackNode> findAllByTrackIdOrderByDisplayOrderAsc(UUID trackId);

    Optional<CareerTrackNode> findByTrackIdAndPositionIdAndGradeId(UUID trackId, UUID positionId, UUID gradeId);

    boolean existsByTrackIdAndPositionIdAndGradeId(UUID trackId, UUID positionId, UUID gradeId);

    boolean existsByTrackIdAndPositionIdAndGradeIdAndIdNot(UUID trackId, UUID positionId, UUID gradeId, UUID id);

    List<CareerTrackNode> findAllByTrackIdAndIsEntryPointTrue(UUID trackId);

}
