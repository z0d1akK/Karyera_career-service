package com.zodiakk.careerservice.integration.consumer;

import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.integration.dto.IntegrationEventEnvelope;
import com.zodiakk.careerservice.integration.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inbound Kafka consumers")
class EventConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Value("${app.kafka.topics.employee-events}")
    private String employeeEventsTopic;

    @Value("${app.kafka.topics.analytics-events}")
    private String analyticsEventsTopic;

    @Test
    @DisplayName("processes employee and analytics events")
    void shouldProcessEmployeeAndAnalyticsEvents() throws Exception {
        UUID skillUpdated = publish(
                employeeEventsTopic,
                KafkaEventConstants.EMPLOYEE_SKILL_UPDATED,
                "EMPLOYEE"
        );
        UUID positionChanged = publish(
                employeeEventsTopic,
                KafkaEventConstants.EMPLOYEE_POSITION_CHANGED,
                "EMPLOYEE"
        );
        UUID assessmentCompleted = publish(
                analyticsEventsTopic,
                KafkaEventConstants.CAREER_ASSESSMENT_COMPLETED,
                "CAREER_ASSESSMENT"
        );
        UUID screeningCompleted = publish(
                analyticsEventsTopic,
                KafkaEventConstants.TALENT_RESERVE_SCREENING_COMPLETED,
                "TALENT_RESERVE_SCREENING"
        );

        awaitProcessed(skillUpdated, positionChanged, assessmentCompleted, screeningCompleted);

        assertThat(processedEventRepository.findById(skillUpdated)).isPresent();
        assertThat(processedEventRepository.findById(positionChanged)).isPresent();
        assertThat(processedEventRepository.findById(assessmentCompleted)).isPresent();
        assertThat(processedEventRepository.findById(screeningCompleted)).isPresent();
    }

    private UUID publish(String topic, String eventType, String aggregateType) throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        IntegrationEventEnvelope envelope = new IntegrationEventEnvelope(
                eventId,
                aggregateId,
                eventType,
                aggregateType,
                KafkaEventConstants.EVENT_VERSION_V1,
                OffsetDateTime.now(ZoneOffset.UTC),
                objectMapper.readTree("{\"source\":\"integration-test\"}")
        );

        kafkaTemplate.send(topic, aggregateId.toString(), toJson(envelope))
                .get(15, TimeUnit.SECONDS);

        return eventId;
    }

    private void awaitProcessed(UUID... eventIds) {
        Set<UUID> pending = Set.copyOf(java.util.List.of(eventIds));
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();

        while (System.nanoTime() < deadline) {
            boolean done = true;

            for (UUID eventId : pending) {
                if (processedEventRepository.findById(eventId).isEmpty()) {
                    done = false;
                    break;
                }
            }

            if (done) {
                return;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        throw new AssertionError("Events were not processed within 20s: " + pending.stream()
                .filter(eventId -> processedEventRepository.findById(eventId).isEmpty())
                .toList());
    }
}
