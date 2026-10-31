package com.zodiakk.careerservice.career.dto.request.create;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCareerGoalCreateRequestDto {

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Target position id")
    @NotNull(message = ValidationMessages.TARGET_POSITION_ID_REQUIRED)
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    @NotNull(message = ValidationMessages.TARGET_GRADE_ID_REQUIRED)
    private UUID targetGradeId;

    @Schema(description = "Target node id")
    private UUID targetNodeId;

    @Schema(description = "Motivation")
    private String motivation;

    @Schema(description = "Target date")
    private LocalDate targetDate;
}
