package com.zodiakk.careerservice.integration.analytics.dto;

import java.util.UUID;

public record ScreeningStartRequestDto(
        UUID reserveId,
        UUID targetPositionId,
        UUID targetGradeId,
        UUID requirementId
) {
}
