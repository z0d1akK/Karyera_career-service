package com.zodiakk.careerservice.career.dto.request.action;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredSkillItemDto {

    @Schema(description = "Skill id")
    @NotNull(message = ValidationMessages.SKILL_ID_REQUIRED)
    private UUID skillId;

    @Schema(description = "Required level")
    @NotNull(message = ValidationMessages.REQUIRED_LEVEL_REQUIRED)
    @Min(value = 1, message = ValidationMessages.REQUIRED_LEVEL_REQUIRED)
    private Integer requiredLevel;

    @Schema(description = "Weight")
    @NotNull(message = ValidationMessages.WEIGHT_REQUIRED)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal weight;

    @Schema(description = "Mandatory")
    @NotNull(message = ValidationMessages.MANDATORY_REQUIRED)
    private Boolean mandatory;

    @Schema(description = "Description")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String description;
}
