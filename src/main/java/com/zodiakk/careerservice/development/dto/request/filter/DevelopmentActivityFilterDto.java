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
public class DevelopmentActivityFilterDto {

    @Schema(description = "Plan id")
    private UUID planId;

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Activity type")
    private DevelopmentActivityType activityType;

    @Schema(description = "Status")
    private DevelopmentActivityStatus status;

    @Schema(description = "Due date from (inclusive)")
    private LocalDate dueDateFrom;

    @Schema(description = "Due date to (inclusive)")
    private LocalDate dueDateTo;
}
