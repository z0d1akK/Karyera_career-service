package com.zodiakk.careerservice.development.dto.request.update;

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
public class PlanReviewUpdateRequestDto {

    @Schema(description = "Reviewer employee id")
    private UUID reviewerEmployeeId;

    @Schema(description = "Decision")
    private PlanReviewDecision decision;

    @Schema(description = "Comment")
    private String comment;

    @Schema(description = "Reviewed at")
    private OffsetDateTime reviewedAt;
}
