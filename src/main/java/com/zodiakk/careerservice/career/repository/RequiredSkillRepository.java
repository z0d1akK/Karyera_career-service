package com.zodiakk.careerservice.career.repository;

import com.zodiakk.careerservice.career.entity.RequiredSkill;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface RequiredSkillRepository extends JpaRepository<RequiredSkill, UUID>, JpaSpecificationExecutor<RequiredSkill> {

    List<RequiredSkill> findAllByRequirementId(UUID requirementId);

    List<RequiredSkill> findAllByRequirementIdAndMandatoryTrue(UUID requirementId);

    Optional<RequiredSkill> findByRequirementIdAndSkillId(UUID requirementId, UUID skillId);

    boolean existsByRequirementIdAndSkillId(UUID requirementId, UUID skillId);

    void deleteAllByRequirementId(UUID requirementId);

}
