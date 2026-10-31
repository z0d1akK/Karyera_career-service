package com.zodiakk.careerservice.career.dto.response;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionRequirementResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Position id")
    private UUID positionId;

    @Schema(description = "Grade id")
    private UUID gradeId;

    @Schema(description = "Revision")
    private Integer revision;

    @Schema(description = "Status")
    private RequirementStatus status;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Min experience months")
    private Integer minExperienceMonths;

    @Schema(description = "Published at")
    private OffsetDateTime publishedAt;

    @Schema(description = "Published by")
    private UUID publishedBy;

    @Schema(description = "Required skills")
    private List<RequiredSkillResponseDto> skills;
}
