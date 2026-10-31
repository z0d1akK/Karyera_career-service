package com.zodiakk.careerservice.development.dto.request.update;

import com.zodiakk.careerservice.common.enums.*;
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
public class DevelopmentActivityUpdateRequestDto {

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Title")
    @Size(max = 200, message = ValidationMessages.TITLE_SIZE)
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
}
