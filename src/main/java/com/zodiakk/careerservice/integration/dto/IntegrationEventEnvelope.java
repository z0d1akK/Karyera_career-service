package com.zodiakk.careerservice.integration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IntegrationEventEnvelope(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        String aggregateType,
        @JsonAlias("schemaVersion") int eventVersion,
        OffsetDateTime occurredAt,
        JsonNode payload
) {
}
