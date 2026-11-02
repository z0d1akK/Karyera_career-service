package com.zodiakk.careerservice.talent.dto.response;

import com.zodiakk.careerservice.integration.analytics.dto.ScreeningResultDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentScreeningResponseDto {

    @Schema(description = "Reserve id")
    private UUID reserveId;

    @Schema(description = "Analytics screening job id")
    private UUID jobId;

    @Schema(description = "Screening job status")
    private String status;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Requirement id used for the screening")
    private UUID requirementId;

    @Schema(description = "Requirement revision used for the screening")
    private Integer requirementRevision;

    @Schema(description = "Methodology version")
    private String methodologyVersion;

    @Schema(description = "Number of candidates selected for the screening")
    private Integer candidateCount;

    @Schema(description = "Number of evaluated candidates")
    private Integer evaluatedCount;

    @Schema(description = "Number of failed evaluations")
    private Integer failedCount;

    @Schema(description = "Screening started at")
    private OffsetDateTime createdAt;

    @Schema(description = "Screening completed at")
    private OffsetDateTime completedAt;

    @Schema(description = "Screening results ordered by rank")
    private List<ScreeningResultDto> results;
}
