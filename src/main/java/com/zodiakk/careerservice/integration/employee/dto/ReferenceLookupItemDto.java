package com.zodiakk.careerservice.integration.employee.dto;

import java.util.UUID;

public record ReferenceLookupItemDto(UUID id, String name, Boolean active) {
}
