package com.zodiakk.careerservice.talent.dto.request.update;

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
public class ReserveDecisionUpdateRequestDto {

    @Schema(description = "Decided by employee id")
    private UUID decidedByEmployeeId;

    @Schema(description = "Decision")
    private ReserveDecisionType decision;

    @Schema(description = "Comment")
    private String comment;

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Decided at")
    private OffsetDateTime decidedAt;
}
