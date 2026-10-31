package com.zodiakk.careerservice.development.dto.request.filter;

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
public class DevelopmentPlanFilterDto {

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Career goal id")
    private UUID careerGoalId;

    @Schema(description = "Status")
    private DevelopmentPlanStatus status;

    @Schema(description = "Start date from (inclusive)")
    private LocalDate startDateFrom;

    @Schema(description = "Start date to (inclusive)")
    private LocalDate startDateTo;

    @Schema(description = "Target date from (inclusive)")
    private LocalDate targetDateFrom;

    @Schema(description = "Target date to (inclusive)")
    private LocalDate targetDateTo;
}
