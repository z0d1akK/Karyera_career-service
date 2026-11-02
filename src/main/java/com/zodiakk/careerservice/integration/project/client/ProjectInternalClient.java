package com.zodiakk.careerservice.integration.project.client;

import com.zodiakk.careerservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.careerservice.integration.project.dto.MemberEligibilityDto;
import com.zodiakk.careerservice.integration.project.dto.ProjectHistoryResponseDto;
import com.zodiakk.careerservice.integration.project.dto.TaskCompletionEvidenceDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "project-service",
        url = "${services.project.url}",
        configuration = IntegrationFeignConfig.class
)
public interface ProjectInternalClient {

    @GetMapping("/internal/v1/projects/{projectId}/members/{employeeId}/eligibility")
    MemberEligibilityDto getMemberEligibility(
            @PathVariable UUID projectId,
            @PathVariable UUID employeeId
    );

    @GetMapping("/internal/v1/tasks/{taskId}/completion-evidence")
    TaskCompletionEvidenceDto getTaskCompletionEvidence(
            @PathVariable UUID taskId,
            @RequestParam UUID employeeId
    );

    @GetMapping("/internal/v1/employees/{employeeId}/project-history")
    ProjectHistoryResponseDto getProjectHistory(@PathVariable UUID employeeId);
}
