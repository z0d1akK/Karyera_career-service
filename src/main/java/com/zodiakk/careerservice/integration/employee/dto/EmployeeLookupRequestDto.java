package com.zodiakk.careerservice.integration.employee.dto;

import java.util.List;
import java.util.UUID;

public record EmployeeLookupRequestDto(List<UUID> employeeIds) {
}
