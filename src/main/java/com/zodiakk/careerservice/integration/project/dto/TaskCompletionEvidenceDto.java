package com.zodiakk.careerservice.integration.project.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskCompletionEvidenceDto(
        UUID taskId,
        UUID employeeId,
        boolean taskCompleted,
        boolean employeeParticipated,
        OffsetDateTime completedAt
) {
}
