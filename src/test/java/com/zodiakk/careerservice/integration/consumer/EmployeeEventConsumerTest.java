package com.zodiakk.careerservice.integration.consumer;

import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.integration.dto.IntegrationEventEnvelope;
import com.zodiakk.careerservice.integration.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeEventConsumerTest {

    private static final UUID EVENT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final UUID AGGREGATE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private static final String MESSAGE = "{\"eventId\":\"dddddddd-dddd-dddd-dddd-dddddddddddd\"}";

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private EmployeeEventConsumer consumer;

    @Nested
    @DisplayName("consume")
    class Consume {

        @ParameterizedTest
        @ValueSource(strings = {
                KafkaEventConstants.EMPLOYEE_SKILL_UPDATED,
                KafkaEventConstants.EMPLOYEE_POSITION_CHANGED
        })
        @DisplayName("registers a supported v1 employee event")
        void shouldConsumeSupportedEvent(String eventType) {
            when(objectMapper.readValue(MESSAGE, IntegrationEventEnvelope.class))
                    .thenReturn(envelope(eventType, KafkaEventConstants.EVENT_VERSION_V1));
            when(processedEventRepository.registerIfAbsent(EVENT_ID)).thenReturn(1);

            assertThatCode(() -> consumer.consume(MESSAGE)).doesNotThrowAnyException();

            verify(processedEventRepository).registerIfAbsent(EVENT_ID);
        }

        @Test
        @DisplayName("skips an event that was already registered")
        void shouldSkipAlreadyProcessedEvent() {
            when(objectMapper.readValue(MESSAGE, IntegrationEventEnvelope.class))
                    .thenReturn(envelope(
                            KafkaEventConstants.EMPLOYEE_SKILL_UPDATED,
                            KafkaEventConstants.EVENT_VERSION_V1
                    ));
            when(processedEventRepository.registerIfAbsent(EVENT_ID)).thenReturn(0);

            assertThatCode(() -> consumer.consume(MESSAGE)).doesNotThrowAnyException();

            verify(processedEventRepository).registerIfAbsent(EVENT_ID);
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects unsupported event versions before registering the event")
        void shouldRejectUnsupportedVersion() {
            when(objectMapper.readValue(MESSAGE, IntegrationEventEnvelope.class))
                    .thenReturn(envelope(KafkaEventConstants.EMPLOYEE_SKILL_UPDATED, 2));

            assertThatThrownBy(() -> consumer.consume(MESSAGE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported employee event version: 2");

            verifyNoInteractions(processedEventRepository);
        }

        @Test
        @DisplayName("rejects unsupported event types")
        void shouldRejectUnsupportedEventType() {
            when(objectMapper.readValue(MESSAGE, IntegrationEventEnvelope.class))
                    .thenReturn(envelope("EmployeeUpdated", KafkaEventConstants.EVENT_VERSION_V1));

            assertThatThrownBy(() -> consumer.consume(MESSAGE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unsupported employee event type: EmployeeUpdated");

            verifyNoInteractions(processedEventRepository);
        }

        @Test
        @DisplayName("propagates malformed messages so they can be sent to the DLT")
        void shouldPropagateMalformedMessage() {
            when(objectMapper.readValue(MESSAGE, IntegrationEventEnvelope.class))
                    .thenThrow(new StreamConstraintsException("malformed"));

            assertThatThrownBy(() -> consumer.consume(MESSAGE))
                    .isInstanceOf(StreamConstraintsException.class);

            verifyNoInteractions(processedEventRepository);
        }
    }

    private IntegrationEventEnvelope envelope(String eventType, int eventVersion) {
        return new IntegrationEventEnvelope(
                EVENT_ID,
                AGGREGATE_ID,
                eventType,
                "EMPLOYEE",
                eventVersion,
                OffsetDateTime.now(ZoneOffset.UTC),
                null
        );
    }
}
