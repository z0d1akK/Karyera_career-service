package com.zodiakk.careerservice.development.dto.request.action;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevelopmentPlanCreateRequestDto {

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Career goal id")
    private UUID careerGoalId;

    @Schema(description = "Title")
    @NotBlank(message = ValidationMessages.TITLE_REQUIRED)
    @Size(max = 200, message = ValidationMessages.TITLE_SIZE)
    private String title;

    @Schema(description = "Description")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String description;

    @Schema(description = "Start date")
    @NotNull(message = ValidationMessages.START_DATE_REQUIRED)
    private LocalDate startDate;

    @Schema(description = "Target date")
    @NotNull(message = ValidationMessages.TARGET_DATE_REQUIRED)
    private LocalDate targetDate;

    @Schema(description = "Initial development activities")
    @Valid
    private List<DevelopmentPlanActivityItemDto> activities;
}
