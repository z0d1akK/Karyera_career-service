package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.CareerTrackRevision;
import com.zodiakk.careerservice.common.enums.*;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CareerTrackRevisionRepository extends JpaRepository<CareerTrackRevision, UUID>, JpaSpecificationExecutor<CareerTrackRevision> {

    List<CareerTrackRevision> findAllByTrackIdOrderByRevisionDesc(UUID trackId);

    Optional<CareerTrackRevision> findByTrackIdAndRevision(UUID trackId, Integer revision);

    Optional<CareerTrackRevision> findTopByTrackIdOrderByRevisionDesc(UUID trackId);

    Optional<CareerTrackRevision> findFirstByTrackIdAndStatusOrderByRevisionDesc(
            UUID trackId,
            CareerRevisionStatus status
    );

    boolean existsByTrackIdAndRevision(UUID trackId, Integer revision);

}
