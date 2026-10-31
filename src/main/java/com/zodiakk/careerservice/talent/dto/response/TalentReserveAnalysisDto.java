package com.zodiakk.careerservice.talent.dto.response;

import com.zodiakk.careerservice.common.enums.TalentReserveStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentReserveAnalysisDto {

    @Schema(description = "Reserve id")
    private UUID reserveId;

    @Schema(description = "Reserve status")
    private TalentReserveStatus status;

    @Schema(description = "Number of candidates")
    private Long candidateCount;

    @Schema(description = "Number of candidates in active statuses")
    private Long activeCandidateCount;

    @Schema(description = "Number of approved candidates")
    private Long approvedCandidateCount;

    @Schema(description = "Candidate count grouped by status")
    private Map<String, Long> byStatus;

    @Schema(description = "Candidate count grouped by current position name")
    private Map<String, Long> byPosition;

    @Schema(description = "Candidate count grouped by current grade name")
    private Map<String, Long> byGrade;
}
