package com.zodiakk.careerservice.integration.project.dto;

import java.util.List;
import java.util.UUID;

public record ProjectHistoryResponseDto(
        UUID employeeId,
        List<ProjectHistoryEntryDto> projects
) {
}
