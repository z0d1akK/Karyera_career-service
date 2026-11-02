package com.zodiakk.careerservice.integration.analytics.service;

import com.zodiakk.careerservice.common.exception.IntegrationUnavailableException;
import com.zodiakk.careerservice.integration.analytics.client.AnalyticsScreeningClient;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartRequestDto;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartResponseDto;
import com.zodiakk.careerservice.integration.analytics.dto.TalentScreeningDto;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsIntegrationServiceTest {

    private static final String UNAVAILABLE =
            "Сервис аналитики недоступен. Предварительный отбор временно невозможен";

    private static final UUID RESERVE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID POSITION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID GRADE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID REQUIREMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID JOB_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private AnalyticsScreeningClient client;

    @InjectMocks
    private AnalyticsIntegrationService service;

    @Nested
    @DisplayName("startScreening")
    class StartScreening {

        @Test
        @DisplayName("returns the screening job started by the analytics service")
        void shouldStartScreening() {
            ScreeningStartRequestDto request = request();
            ScreeningStartResponseDto response = new ScreeningStartResponseDto(JOB_ID, "RUNNING");
            when(client.startScreening(request)).thenReturn(response);

            assertThat(service.startScreening(request)).isSameAs(response);
        }

        @Test
        @DisplayName("translates a Feign failure into IntegrationUnavailableException")
        void shouldTranslateFailure() {
            ScreeningStartRequestDto request = request();
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.startScreening(request)).thenThrow(failure);

            assertThatThrownBy(() -> service.startScreening(request))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage(UNAVAILABLE)
                    .hasCause(failure);
        }
    }

    @Nested
    @DisplayName("getScreening")
    class GetScreening {

        @Test
        @DisplayName("returns the screening job from the analytics service")
        void shouldReturnScreening() {
            TalentScreeningDto screening = new TalentScreeningDto(
                    JOB_ID,
                    null,
                    JOB_ID,
                    RESERVE_ID,
                    POSITION_ID,
                    GRADE_ID,
                    REQUIREMENT_ID,
                    1,
                    "v1",
                    "COMPLETED",
                    1,
                    1,
                    0,
                    null,
                    List.of()
            );
            when(client.getScreening(JOB_ID)).thenReturn(screening);

            assertThat(service.getScreening(JOB_ID)).isSameAs(screening);
        }

        @Test
        @DisplayName("translates a Feign failure into IntegrationUnavailableException")
        void shouldTranslateFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.getScreening(JOB_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getScreening(JOB_ID))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage(UNAVAILABLE)
                    .hasCause(failure);
        }
    }

    private ScreeningStartRequestDto request() {
        return new ScreeningStartRequestDto(RESERVE_ID, POSITION_ID, GRADE_ID, REQUIREMENT_ID);
    }
}
