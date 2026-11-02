package com.zodiakk.careerservice.integration.consumer;

import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.integration.dto.IntegrationEventEnvelope;
import com.zodiakk.careerservice.integration.repository.ProcessedEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeEventConsumer {

    private final ObjectMapper objectMapper;

    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.employee-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(String message) {
        IntegrationEventEnvelope envelope =
                objectMapper.readValue(message, IntegrationEventEnvelope.class);

        if (envelope.eventVersion() != KafkaEventConstants.EVENT_VERSION_V1) {
            throw new IllegalArgumentException(
                    "Unsupported employee event version: " + envelope.eventVersion()
            );
        }

        if (!KafkaEventConstants.EMPLOYEE_SKILL_UPDATED.equals(envelope.eventType())
                && !KafkaEventConstants.EMPLOYEE_POSITION_CHANGED.equals(envelope.eventType())) {
            throw new IllegalArgumentException(
                    "Unsupported employee event type: " + envelope.eventType()
            );
        }

        if (processedEventRepository.registerIfAbsent(envelope.eventId()) == 0) {
            log.debug("Employee event already processed: eventId={}", envelope.eventId());
            return;
        }

        /*
         * Career Service stores no copy of employee skills or positions: they are always read from
         * Employee Service on demand. The event is consumed to keep the shared contract and the
         * processed_events bookkeeping in place, so that later projections can be added safely.
         */
        log.info(
                "Processed employee event: eventId={}, type={}, aggregateId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateId()
        );
    }
}
