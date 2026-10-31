package com.zodiakk.careerservice.development.dto.request.action;

import com.zodiakk.careerservice.common.enums.PlanReviewDecision;
import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevelopmentPlanReviewRequestDto {

    @Schema(description = "Review decision", example = "APPROVED")
    @NotNull(message = ValidationMessages.DECISION_REQUIRED)
    private PlanReviewDecision decision;

    @Schema(description = "Review comment")
    @Size(max = 5000, message = ValidationMessages.COMMENT_SIZE)
    private String comment;

    @Schema(description = "Expected entity version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
