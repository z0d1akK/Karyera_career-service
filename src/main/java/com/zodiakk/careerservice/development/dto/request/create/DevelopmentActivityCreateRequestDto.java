package com.zodiakk.careerservice.development.dto.request.create;

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
public class DevelopmentActivityCreateRequestDto {

    @Schema(description = "Plan id")
    @NotNull(message = ValidationMessages.PLAN_ID_REQUIRED)
    private UUID planId;

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Title")
    @NotBlank(message = ValidationMessages.TITLE_REQUIRED)
    @Size(max = 200, message = ValidationMessages.TITLE_SIZE)
    private String title;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Activity type")
    @NotNull(message = ValidationMessages.ACTIVITY_TYPE_REQUIRED)
    private DevelopmentActivityType activityType;

    @Schema(description = "Due date")
    private LocalDate dueDate;
}
