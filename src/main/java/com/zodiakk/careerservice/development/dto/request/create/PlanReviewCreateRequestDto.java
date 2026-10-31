package com.zodiakk.careerservice.development.dto.request.create;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanReviewCreateRequestDto {

    @Schema(description = "Plan id")
    @NotNull(message = ValidationMessages.PLAN_ID_REQUIRED)
    private UUID planId;

    @Schema(description = "Decision")
    @NotNull(message = ValidationMessages.DECISION_REQUIRED)
    private PlanReviewDecision decision;

    @Schema(description = "Comment")
    private String comment;
}
