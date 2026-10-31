package com.zodiakk.careerservice.talent.dto.response;

import com.zodiakk.careerservice.common.enums.TalentReserveStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentReserveContextDto {

    @Schema(description = "Reserve id")
    private UUID reserveId;

    @Schema(description = "Reserve name")
    private String name;

    @Schema(description = "Reserve status")
    private TalentReserveStatus status;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Effective from")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective to")
    private LocalDate effectiveTo;

    @Schema(description = "Number of candidates")
    private Long candidateCount;

    @Schema(description = "Number of candidates in active statuses")
    private Long activeCandidateCount;

    @Schema(description = "Number of approved candidates")
    private Long approvedCandidateCount;
}
