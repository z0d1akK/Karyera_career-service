package com.zodiakk.careerservice.integration.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectHistoryEntryDto(
        UUID projectId,
        String projectCode,
        String projectName,
        String projectStatus,
        UUID projectRoleId,
        String projectRoleName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal allocationPercent,
        boolean active
) {
}
