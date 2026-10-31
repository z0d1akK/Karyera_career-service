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
public class DevelopmentPlanUpdateRequestDto {

    @Schema(description = "Career goal id")
    private UUID careerGoalId;

    @Schema(description = "Title")
    @Size(max = 200, message = ValidationMessages.TITLE_SIZE)
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
}
