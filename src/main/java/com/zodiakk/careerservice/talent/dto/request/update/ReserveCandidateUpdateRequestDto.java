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
public class ReserveCandidateUpdateRequestDto {

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Status")
    private ReserveCandidateStatus status;

    @Schema(description = "Nominated by")
    private UUID nominatedBy;

    @Schema(description = "Nominated at")
    private OffsetDateTime nominatedAt;

    @Schema(description = "Included at")
    private OffsetDateTime includedAt;

    @Schema(description = "Removed at")
    private OffsetDateTime removedAt;

    @Schema(description = "Comment")
    private String comment;
}
