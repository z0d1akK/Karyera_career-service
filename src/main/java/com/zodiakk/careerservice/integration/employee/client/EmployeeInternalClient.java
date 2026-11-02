package com.zodiakk.careerservice.integration.employee.client;

import com.zodiakk.careerservice.integration.config.IntegrationFeignConfig;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeAccessScopeDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupRequestDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupResponseDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupRequestDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "employee-service",
        url = "${services.employee.url}",
        configuration = IntegrationFeignConfig.class
)
public interface EmployeeInternalClient {

    @PostMapping("/internal/v1/employees/lookup")
    EmployeeLookupResponseDto lookupEmployees(@RequestBody EmployeeLookupRequestDto request);

    @PostMapping("/internal/v1/positions/lookup")
    ReferenceLookupResponseDto lookupPositions(@RequestBody ReferenceLookupRequestDto request);

    @PostMapping("/internal/v1/grades/lookup")
    ReferenceLookupResponseDto lookupGrades(@RequestBody ReferenceLookupRequestDto request);

    @PostMapping("/internal/v1/skills/lookup")
    ReferenceLookupResponseDto lookupSkills(@RequestBody ReferenceLookupRequestDto request);

    @GetMapping("/internal/v1/employees/by-user/{userId}")
    EmployeeLookupItemDto getByUserId(@PathVariable UUID userId);

    @GetMapping("/internal/v1/employees/{employeeId}/access-scope")
    EmployeeAccessScopeDto getAccessScope(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) UUID actorEmployeeId
    );
}
