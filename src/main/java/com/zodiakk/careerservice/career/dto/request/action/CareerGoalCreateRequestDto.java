package com.zodiakk.careerservice.career.dto.request.action;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerGoalCreateRequestDto {

    @Schema(description = "Target position id")
    @NotNull(message = ValidationMessages.TARGET_POSITION_ID_REQUIRED)
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    @NotNull(message = ValidationMessages.TARGET_GRADE_ID_REQUIRED)
    private UUID targetGradeId;

    @Schema(description = "Target career track node id")
    private UUID targetNodeId;

    @Schema(description = "Target date")
    private LocalDate targetDate;

    @Schema(description = "Motivation")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String motivation;
}
