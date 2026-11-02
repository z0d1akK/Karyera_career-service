package com.zodiakk.careerservice.integration.analytics.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScreeningResultDto(
        UUID id,
        OffsetDateTime createdAt,
        UUID screeningId,
        UUID employeeId,
        UUID assessmentId,
        Integer rank,
        BigDecimal score,
        String eligibilityStatus,
        Boolean mandatoryRequirementsMet,
        String exclusionReason
) {
}
