package com.zodiakk.careerservice.talent.repository;

import com.zodiakk.careerservice.talent.entity.ReserveDecision;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ReserveDecisionRepository extends JpaRepository<ReserveDecision, UUID>, JpaSpecificationExecutor<ReserveDecision> {

    List<ReserveDecision> findAllByCandidateIdOrderByDecidedAtDesc(UUID candidateId);

}
