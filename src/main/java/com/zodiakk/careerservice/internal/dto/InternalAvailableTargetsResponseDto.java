package com.zodiakk.careerservice.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Career targets available to an employee inside a track")
public record InternalAvailableTargetsResponseDto(
        UUID trackId,
        UUID employeeId,
        UUID currentNodeId,
        List<Target> targets
) {

    @Schema(description = "Reachable career target")
    public record Target(
            UUID nodeId,
            UUID positionId,
            UUID gradeId,
            String label,
            String transitionType,
            Integer estimatedMonths
    ) {
    }
}
