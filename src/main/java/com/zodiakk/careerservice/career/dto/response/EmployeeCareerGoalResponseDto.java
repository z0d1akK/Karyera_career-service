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
public class EmployeeCareerGoalResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Created by")
    private UUID createdBy;

    @Schema(description = "Employee id")
    private UUID employeeId;

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

    @Schema(description = "Resolved target position name")
    private String targetPositionName;

    @Schema(description = "Resolved target grade name")
    private String targetGradeName;
}
