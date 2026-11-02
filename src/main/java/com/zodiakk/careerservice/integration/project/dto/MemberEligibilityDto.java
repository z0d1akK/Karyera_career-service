package com.zodiakk.careerservice.integration.project.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MemberEligibilityDto(
        UUID projectId,
        UUID employeeId,
        boolean eligible,
        boolean projectActive,
        boolean assignmentActive,
        UUID assignmentId,
        LocalDate assignmentStartDate,
        LocalDate assignmentEndDate,
        String reason
) {
}
