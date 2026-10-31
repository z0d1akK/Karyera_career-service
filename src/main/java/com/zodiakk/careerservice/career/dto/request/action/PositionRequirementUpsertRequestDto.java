package com.zodiakk.careerservice.career.dto.request.action;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionRequirementUpsertRequestDto {

    @Schema(description = "Position id")
    @NotNull(message = ValidationMessages.POSITION_ID_REQUIRED)
    private UUID positionId;

    @Schema(description = "Grade id")
    @NotNull(message = ValidationMessages.GRADE_ID_REQUIRED)
    private UUID gradeId;

    @Schema(description = "Description")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String description;

    @Schema(description = "Minimum required experience in months")
    @NotNull(message = ValidationMessages.MIN_EXPERIENCE_MONTHS_REQUIRED)
    @Min(value = 0, message = ValidationMessages.ESTIMATED_MONTHS_MIN)
    private Integer minExperienceMonths;

    @Schema(description = "Required skills")
    @Valid
    private List<RequiredSkillItemDto> skills;
}
