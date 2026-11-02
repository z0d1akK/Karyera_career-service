package com.zodiakk.careerservice.outbox;

import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.housekeeping.HousekeepingService;
import com.zodiakk.careerservice.integration.repository.ProcessedEventRepository;
import com.zodiakk.careerservice.outbox.entity.OutboxEvent;
import com.zodiakk.careerservice.outbox.entity.OutboxEventStatus;
import com.zodiakk.careerservice.outbox.publisher.OutboxPublisher;
import com.zodiakk.careerservice.outbox.repository.OutboxEventRepository;
import com.zodiakk.careerservice.outbox.service.OutboxClaimService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Transactional outbox")
class OutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private OutboxClaimService outboxClaimService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private HousekeepingService housekeepingService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    @DisplayName("refuses to record an event outside a transaction")
    void shouldRequireTransaction() {
        assertThatThrownBy(() -> outboxEventService.saveEvent(
                UUID.randomUUID(),
                KafkaEventConstants.AGGREGATE_TYPE_CAREER_TRACK,
                KafkaEventConstants.CAREER_TRACK_PUBLISHED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of("code", "BE")
        )).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("claims a pending event and marks it published")
    void shouldClaimAndMarkPublished() {
        UUID eventId = saveEvent();

        OutboxEvent claimed = outboxClaimService.claimBatch().getFirst();

        assertThat(claimed.getId()).isEqualTo(eventId);
        assertThat(claimed.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
        assertThat(claimed.getAttempts()).isEqualTo(1);
        assertThat(outboxClaimService.markPublished(claimed.getId(), claimed.getAttempts())).isTrue();

        OutboxEvent stored = outboxEventRepository.findById(eventId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(stored.getPublishedAt()).isNotNull();
        assertThat(stored.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("returns a claimed event to the pending queue after a failure")
    void shouldClaimAndMarkFailed() {
        UUID eventId = saveEvent();
        OutboxEvent claimed = outboxClaimService.claimBatch().getFirst();

        assertThat(outboxClaimService.markFailed(claimed.getId(), claimed.getAttempts())).isTrue();

        OutboxEvent stored = outboxEventRepository.findById(eventId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(stored.getLockedUntil()).isNull();
        assertThat(stored.getNextAttemptAt()).isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("publishes a pending event until it is marked published")
    void shouldPublishPendingEvents() {
        UUID eventId = saveEvent();

        outboxPublisher.publishPendingEvents();
        awaitPublished(eventId, Duration.ofSeconds(20));

        OutboxEvent stored = outboxEventRepository.findById(eventId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(stored.getPublishedAt()).isNotNull();
        assertThat(stored.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("removes published outbox rows and processed events past retention")
    void shouldCleanupExpiredRecords() {
        UUID recentEventId = saveEvent();
        OutboxEvent claimed = outboxClaimService.claimBatch().getFirst();
        assertThat(outboxClaimService.markPublished(claimed.getId(), claimed.getAttempts())).isTrue();

        UUID expiredEventId = UUID.randomUUID();
        outboxEventRepository.saveAndFlush(OutboxEvent.builder()
                .id(expiredEventId)
                .aggregateId(UUID.randomUUID())
                .aggregateType(KafkaEventConstants.AGGREGATE_TYPE_CAREER_TRACK)
                .eventType(KafkaEventConstants.CAREER_TRACK_PUBLISHED)
                .eventVersion(KafkaEventConstants.EVENT_VERSION_V1)
                .payload("{}")
                .status(OutboxEventStatus.PUBLISHED)
                .attempts(1)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(40))
                .publishedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(40))
                .nextAttemptAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(40))
                .build());

        UUID expiredProcessedId = UUID.randomUUID();
        UUID recentProcessedId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?)",
                expiredProcessedId,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(60)
        );
        jdbcTemplate.update(
                "INSERT INTO processed_events (event_id, processed_at) VALUES (?, ?)",
                recentProcessedId,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        housekeepingService.cleanup();

        assertThat(outboxEventRepository.findById(expiredEventId)).isEmpty();
        assertThat(outboxEventRepository.findById(recentEventId)).isPresent();
        assertThat(processedEventRepository.findById(expiredProcessedId)).isEmpty();
        assertThat(processedEventRepository.findById(recentProcessedId)).isPresent();
    }

    private UUID saveEvent() {
        UUID aggregateId = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> outboxEventService.saveEvent(
                aggregateId,
                KafkaEventConstants.AGGREGATE_TYPE_CAREER_TRACK,
                KafkaEventConstants.CAREER_TRACK_PUBLISHED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of("trackId", aggregateId, "code", "BE", "revision", 1)
        ));

        return outboxEventRepository.findAll().getFirst().getId();
    }

    private void awaitPublished(UUID eventId, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            boolean published = outboxEventRepository.findById(eventId)
                    .map(event -> event.getStatus() == OutboxEventStatus.PUBLISHED)
                    .orElse(false);

            if (published) {
                return;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }

        throw new AssertionError("Outbox event " + eventId + " was not published: "
                + outboxEventRepository.findById(eventId).orElse(null));
    }
}
