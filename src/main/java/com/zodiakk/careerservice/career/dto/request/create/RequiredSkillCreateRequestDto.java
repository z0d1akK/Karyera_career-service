package com.zodiakk.careerservice.career.dto.request.create;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredSkillCreateRequestDto {

    @Schema(description = "Requirement id")
    @NotNull(message = ValidationMessages.REQUIREMENT_ID_REQUIRED)
    private UUID requirementId;

    @Schema(description = "Skill id")
    @NotNull(message = ValidationMessages.SKILL_ID_REQUIRED)
    private UUID skillId;

    @Schema(description = "Required level")
    @NotNull(message = ValidationMessages.REQUIRED_LEVEL_REQUIRED)
    private Integer requiredLevel;

    @Schema(description = "Weight")
    @NotNull(message = ValidationMessages.WEIGHT_REQUIRED)
    private BigDecimal weight;

    @Schema(description = "Mandatory")
    @NotNull(message = ValidationMessages.MANDATORY_REQUIRED)
    private Boolean mandatory;

    @Schema(description = "Description")
    private String description;
}
