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
public class AnalyticsEventConsumer {

    private final ObjectMapper objectMapper;

    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.analytics-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(String message) {
        IntegrationEventEnvelope envelope =
                objectMapper.readValue(message, IntegrationEventEnvelope.class);

        if (envelope.eventVersion() != KafkaEventConstants.EVENT_VERSION_V1) {
            throw new IllegalArgumentException(
                    "Unsupported analytics event version: " + envelope.eventVersion()
            );
        }

        if (!KafkaEventConstants.CAREER_ASSESSMENT_COMPLETED.equals(envelope.eventType())
                && !KafkaEventConstants.TALENT_RESERVE_SCREENING_COMPLETED.equals(
                        envelope.eventType())) {
            throw new IllegalArgumentException(
                    "Unsupported analytics event type: " + envelope.eventType()
            );
        }

        if (processedEventRepository.registerIfAbsent(envelope.eventId()) == 0) {
            log.debug("Analytics event already processed: eventId={}", envelope.eventId());
            return;
        }

        /*
         * Assessment and screening results are owned by Analytics Service and are read through its
         * internal API. Career Service only acknowledges the completion events, so that HR can be
         * notified and the events stay idempotent once a local projection is introduced.
         */
        log.info(
                "Processed analytics event: eventId={}, type={}, aggregateId={}",
                envelope.eventId(),
                envelope.eventType(),
                envelope.aggregateId()
        );
    }
}
