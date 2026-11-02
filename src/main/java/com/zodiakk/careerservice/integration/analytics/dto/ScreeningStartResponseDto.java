package com.zodiakk.careerservice.integration.analytics.dto;

import java.util.UUID;

public record ScreeningStartResponseDto(
        UUID jobId,
        String status
) {
}
