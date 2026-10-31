package com.zodiakk.careerservice.development.dto.response;

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
public class DevelopmentPlanResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Career goal id")
    private UUID careerGoalId;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Status")
    private DevelopmentPlanStatus status;

    @Schema(description = "Start date")
    private LocalDate startDate;

    @Schema(description = "Target date")
    private LocalDate targetDate;

    @Schema(description = "Approved by")
    private UUID approvedBy;

    @Schema(description = "Approved at")
    private OffsetDateTime approvedAt;

    @Schema(description = "Development progress as a percentage")
    private Integer progressPercent;

    @Schema(description = "Number of activities in the plan")
    private Long totalActivities;

    @Schema(description = "Number of verified activities")
    private Long completedActivities;
}
