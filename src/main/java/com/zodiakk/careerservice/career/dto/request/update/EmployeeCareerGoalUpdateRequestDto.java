package com.zodiakk.careerservice.career.dto.request.update;

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
public class EmployeeCareerGoalUpdateRequestDto {

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Target node id")
    private UUID targetNodeId;

    @Schema(description = "Status")
    private CareerGoalStatus status;

    @Schema(description = "Motivation")
    private String motivation;

    @Schema(description = "Target date")
    private LocalDate targetDate;
}
