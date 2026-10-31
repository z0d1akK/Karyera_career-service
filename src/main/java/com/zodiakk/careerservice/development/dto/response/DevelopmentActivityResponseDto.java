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
public class DevelopmentActivityResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Plan id")
    private UUID planId;

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Activity type")
    private DevelopmentActivityType activityType;

    @Schema(description = "Status")
    private DevelopmentActivityStatus status;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Completed at")
    private OffsetDateTime completedAt;

    @Schema(description = "Assigned by")
    private UUID assignedBy;

    @Schema(description = "Activity evidence records")
    private List<ActivityEvidenceResponseDto> evidence;
}
