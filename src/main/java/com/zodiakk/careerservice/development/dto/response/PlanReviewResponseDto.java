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
public class PlanReviewResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Plan id")
    private UUID planId;

    @Schema(description = "Reviewer employee id")
    private UUID reviewerEmployeeId;

    @Schema(description = "Decision")
    private PlanReviewDecision decision;

    @Schema(description = "Comment")
    private String comment;

    @Schema(description = "Reviewed at")
    private OffsetDateTime reviewedAt;
}
