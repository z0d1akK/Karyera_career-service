package com.zodiakk.careerservice.integration.project.service;

import com.zodiakk.careerservice.common.exception.IntegrationUnavailableException;
import com.zodiakk.careerservice.integration.project.client.ProjectInternalClient;
import com.zodiakk.careerservice.integration.project.dto.MemberEligibilityDto;
import com.zodiakk.careerservice.integration.project.dto.ProjectHistoryResponseDto;
import com.zodiakk.careerservice.integration.project.dto.TaskCompletionEvidenceDto;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectDirectoryServiceTest {

    private static final String UNAVAILABLE = "Сервис проектов недоступен. Попробуйте позднее";

    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID TASK_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ProjectInternalClient client;

    @InjectMocks
    private ProjectDirectoryService service;

    @Nested
    @DisplayName("getMemberEligibility")
    class GetMemberEligibility {

        @Test
        @DisplayName("returns member eligibility from the project service")
        void shouldReturnEligibility() {
            MemberEligibilityDto eligibility = new MemberEligibilityDto(
                    PROJECT_ID,
                    EMPLOYEE_ID,
                    true,
                    true,
                    true,
                    null,
                    null,
                    null,
                    null
            );
            when(client.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID)).thenReturn(eligibility);

            assertThat(service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID)).isSameAs(eligibility);
        }

        @Test
        @DisplayName("translates a Feign failure into IntegrationUnavailableException")
        void shouldTranslateFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage(UNAVAILABLE)
                    .hasCause(failure);
        }
    }

    @Nested
    @DisplayName("getTaskCompletionEvidence")
    class GetTaskCompletionEvidence {

        @Test
        @DisplayName("returns task completion evidence from the project service")
        void shouldReturnTaskEvidence() {
            TaskCompletionEvidenceDto evidence = new TaskCompletionEvidenceDto(
                    TASK_ID,
                    EMPLOYEE_ID,
                    true,
                    true,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
            when(client.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID)).thenReturn(evidence);

            assertThat(service.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID)).isSameAs(evidence);
        }

        @Test
        @DisplayName("translates a Feign failure into IntegrationUnavailableException")
        void shouldTranslateFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage(UNAVAILABLE)
                    .hasCause(failure);
        }
    }

    @Nested
    @DisplayName("getProjectHistory")
    class GetProjectHistory {

        @Test
        @DisplayName("returns project history from the project service")
        void shouldReturnProjectHistory() {
            ProjectHistoryResponseDto history = new ProjectHistoryResponseDto(EMPLOYEE_ID, List.of());
            when(client.getProjectHistory(EMPLOYEE_ID)).thenReturn(history);

            assertThat(service.getProjectHistory(EMPLOYEE_ID)).isSameAs(history);
        }

        @Test
        @DisplayName("translates a Feign failure into IntegrationUnavailableException")
        void shouldTranslateFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.getProjectHistory(EMPLOYEE_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getProjectHistory(EMPLOYEE_ID))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage(UNAVAILABLE)
                    .hasCause(failure);
        }
    }
}
