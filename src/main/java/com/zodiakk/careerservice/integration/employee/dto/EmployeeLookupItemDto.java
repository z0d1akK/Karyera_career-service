package com.zodiakk.careerservice.integration.employee.dto;

import java.util.UUID;

public record EmployeeLookupItemDto(
        UUID id,
        String fullName,
        boolean active,
        UUID positionId,
        UUID gradeId
) {
}
