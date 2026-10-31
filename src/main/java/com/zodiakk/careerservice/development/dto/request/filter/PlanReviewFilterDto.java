package com.zodiakk.careerservice.development.dto.request.filter;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanReviewFilterDto {

    @Schema(description = "Plan id")
    private UUID planId;

    @Schema(description = "Reviewer employee id")
    private UUID reviewerEmployeeId;

    @Schema(description = "Decision")
    private PlanReviewDecision decision;
}
