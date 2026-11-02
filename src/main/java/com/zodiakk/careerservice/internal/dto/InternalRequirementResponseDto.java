package com.zodiakk.careerservice.internal.dto;

import com.zodiakk.careerservice.common.enums.RequirementStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Published position requirement for internal consumers")
public record InternalRequirementResponseDto(
        UUID requirementId,
        UUID positionId,
        UUID gradeId,
        Integer revision,
        RequirementStatus status,
        Integer minExperienceMonths,
        String description,
        List<Skill> skills
) {

    @Schema(description = "Required skill of the position requirement")
    public record Skill(
            UUID skillId,
            Integer requiredLevel,
            BigDecimal weight,
            Boolean mandatory,
            String description
    ) {
    }
}
