package com.zodiakk.careerservice.talent.dto.response;

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
public class ReserveCandidateResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Reserve id")
    private UUID reserveId;

    @Schema(description = "Employee id")
    private UUID employeeId;

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

    @Schema(description = "Decisions made for the candidate")
    private List<ReserveDecisionResponseDto> decisions;
}
