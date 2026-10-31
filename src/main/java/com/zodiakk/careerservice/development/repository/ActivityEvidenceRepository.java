package com.zodiakk.careerservice.development.repository;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.development.entity.ActivityEvidence;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ActivityEvidenceRepository extends JpaRepository<ActivityEvidence, UUID>, JpaSpecificationExecutor<ActivityEvidence> {

    List<ActivityEvidence> findAllByActivityId(UUID activityId);

    List<ActivityEvidence> findAllByActivityIdAndVerificationStatus(
            UUID activityId,
            EvidenceVerificationStatus verificationStatus
    );

    long countByActivityId(UUID activityId);

}
