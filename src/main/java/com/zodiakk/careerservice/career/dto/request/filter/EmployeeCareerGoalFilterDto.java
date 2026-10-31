package com.zodiakk.careerservice.career.dto.request.filter;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCareerGoalFilterDto {

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Status")
    private CareerGoalStatus status;
}
