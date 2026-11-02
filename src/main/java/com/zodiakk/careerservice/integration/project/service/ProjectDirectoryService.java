package com.zodiakk.careerservice.integration.project.service;

import com.zodiakk.careerservice.common.exception.IntegrationUnavailableException;
import com.zodiakk.careerservice.integration.project.client.ProjectInternalClient;
import com.zodiakk.careerservice.integration.project.dto.MemberEligibilityDto;
import com.zodiakk.careerservice.integration.project.dto.ProjectHistoryResponseDto;
import com.zodiakk.careerservice.integration.project.dto.TaskCompletionEvidenceDto;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDirectoryService {

    private static final String PROJECT_SERVICE_UNAVAILABLE =
            "Сервис проектов недоступен. Попробуйте позднее";

    private final ProjectInternalClient client;

    public MemberEligibilityDto getMemberEligibility(UUID projectId, UUID employeeId) {
        try {
            return client.getMemberEligibility(projectId, employeeId);
        } catch (FeignException ex) {
            throw unavailable(ex);
        }
    }

    public TaskCompletionEvidenceDto getTaskCompletionEvidence(UUID taskId, UUID employeeId) {
        try {
            return client.getTaskCompletionEvidence(taskId, employeeId);
        } catch (FeignException ex) {
            throw unavailable(ex);
        }
    }

    public ProjectHistoryResponseDto getProjectHistory(UUID employeeId) {
        try {
            return client.getProjectHistory(employeeId);
        } catch (FeignException ex) {
            throw unavailable(ex);
        }
    }

    private IntegrationUnavailableException unavailable(FeignException cause) {
        log.warn("Project service call failed: {}", cause.getMessage());
        return new IntegrationUnavailableException(PROJECT_SERVICE_UNAVAILABLE, cause);
    }
}
