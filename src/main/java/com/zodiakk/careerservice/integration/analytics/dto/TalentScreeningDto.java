package com.zodiakk.careerservice.integration.analytics.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TalentScreeningDto(
        UUID id,
        OffsetDateTime createdAt,
        UUID jobId,
        UUID reserveId,
        UUID targetPositionId,
        UUID targetGradeId,
        UUID requirementId,
        Integer requirementRevision,
        String methodologyVersion,
        String status,
        Integer candidateCount,
        Integer evaluatedCount,
        Integer failedCount,
        OffsetDateTime completedAt,
        List<ScreeningResultDto> results
) {
}
