package com.zodiakk.careerservice.career.dto.request.create;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionRequirementCreateRequestDto {

    @Schema(description = "Position id")
    @NotNull(message = ValidationMessages.POSITION_ID_REQUIRED)
    private UUID positionId;

    @Schema(description = "Grade id")
    @NotNull(message = ValidationMessages.GRADE_ID_REQUIRED)
    private UUID gradeId;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Min experience months")
    @NotNull(message = ValidationMessages.MIN_EXPERIENCE_MONTHS_REQUIRED)
    private Integer minExperienceMonths;
}
