package com.zodiakk.careerservice.development.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevelopmentProgressResponseDto {

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Number of non-cancelled development plans")
    private Long totalPlans;

    @Schema(description = "Number of active plans")
    private Long activePlans;

    @Schema(description = "Number of activities in non-cancelled plans")
    private Long totalActivities;

    @Schema(description = "Number of completed activities")
    private Long completedActivities;

    @Schema(description = "Overall development progress as a percentage")
    private Integer progressPercent;

    @Schema(description = "Plans included in the calculation")
    private List<DevelopmentPlanResponseDto> plans;
}
