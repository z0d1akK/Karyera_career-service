package com.zodiakk.careerservice.integration.employee.service;

import com.zodiakk.careerservice.common.exception.IntegrationUnavailableException;
import com.zodiakk.careerservice.common.exception.domain.ReferenceDataException;
import com.zodiakk.careerservice.integration.employee.client.EmployeeInternalClient;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeAccessScopeDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupRequestDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupResponseDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupRequestDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupResponseDto;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDirectoryService {

    private static final String EMPLOYEE_LOOKUP_FAILED = "Сервис сотрудников недоступен. Попробуйте позднее";
    private static final String ACCESS_SCOPE_FAILED = "Не удалось получить область доступа к сотруднику";

    private final EmployeeInternalClient client;

    public EmployeeLookupItemDto requireEmployee(UUID employeeId) {
        EmployeeLookupItemDto employee = findEmployees(List.of(employeeId)).get(employeeId);

        if (employee == null) {
            throw ReferenceDataException.employeeNotFound(employeeId);
        }

        return employee;
    }

    public Map<UUID, EmployeeLookupItemDto> findEmployees(Collection<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }

        try {
            EmployeeLookupResponseDto response = client.lookupEmployees(
                    new EmployeeLookupRequestDto(List.copyOf(new LinkedHashSet<>(employeeIds)))
            );

            return toEmployeeMap(response);
        } catch (FeignException ex) {
            throw unavailable(EMPLOYEE_LOOKUP_FAILED, ex);
        }
    }

    public ReferenceLookupItemDto requirePosition(UUID positionId) {
        return requireReference(
                findPositions(List.of(positionId)),
                positionId,
                () -> ReferenceDataException.positionNotFound(positionId)
        );
    }

    public ReferenceLookupItemDto requireGrade(UUID gradeId) {
        return requireReference(
                findGrades(List.of(gradeId)),
                gradeId,
                () -> ReferenceDataException.gradeNotFound(gradeId)
        );
    }

    public ReferenceLookupItemDto requireSkill(UUID skillId) {
        return requireReference(
                findSkills(List.of(skillId)),
                skillId,
                () -> ReferenceDataException.skillNotFound(skillId)
        );
    }

    public Map<UUID, ReferenceLookupItemDto> findPositions(Collection<UUID> ids) {
        return lookupReferences(ids, client::lookupPositions);
    }

    public Map<UUID, ReferenceLookupItemDto> findGrades(Collection<UUID> ids) {
        return lookupReferences(ids, client::lookupGrades);
    }

    public Map<UUID, ReferenceLookupItemDto> findSkills(Collection<UUID> ids) {
        return lookupReferences(ids, client::lookupSkills);
    }

    public EmployeeLookupItemDto findByUserId(UUID userId) {
        try {
            return client.getByUserId(userId);
        } catch (FeignException.NotFound ex) {
            throw ReferenceDataException.employeeNotFound(userId);
        } catch (FeignException ex) {
            throw unavailable(EMPLOYEE_LOOKUP_FAILED, ex);
        }
    }

    public EmployeeAccessScopeDto getAccessScope(UUID employeeId, UUID actorEmployeeId) {
        try {
            return client.getAccessScope(employeeId, actorEmployeeId);
        } catch (FeignException.NotFound ex) {
            throw ReferenceDataException.employeeNotFound(employeeId);
        } catch (FeignException ex) {
            throw unavailable(ACCESS_SCOPE_FAILED, ex);
        }
    }

    private ReferenceLookupItemDto requireReference(
            Map<UUID, ReferenceLookupItemDto> resolved,
            UUID id,
            java.util.function.Supplier<RuntimeException> missing
    ) {
        ReferenceLookupItemDto item = resolved.get(id);

        if (item == null) {
            throw missing.get();
        }

        return item;
    }

    private Map<UUID, ReferenceLookupItemDto> lookupReferences(
            Collection<UUID> ids,
            java.util.function.Function<ReferenceLookupRequestDto, ReferenceLookupResponseDto> lookup
    ) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        try {
            ReferenceLookupResponseDto response = lookup.apply(
                    new ReferenceLookupRequestDto(List.copyOf(new LinkedHashSet<>(ids)))
            );

            Map<UUID, ReferenceLookupItemDto> resolved = new HashMap<>();

            if (response != null && response.items() != null) {
                for (ReferenceLookupItemDto item : response.items()) {
                    resolved.put(item.id(), item);
                }
            }

            return resolved;
        } catch (FeignException ex) {
            throw unavailable(EMPLOYEE_LOOKUP_FAILED, ex);
        }
    }

    private Map<UUID, EmployeeLookupItemDto> toEmployeeMap(EmployeeLookupResponseDto response) {
        Map<UUID, EmployeeLookupItemDto> employees = new HashMap<>();

        if (response == null || response.employees() == null) {
            return employees;
        }

        for (EmployeeLookupItemDto employee : response.employees()) {
            employees.put(employee.id(), employee);
        }

        return employees;
    }

    private IntegrationUnavailableException unavailable(String message, FeignException cause) {
        log.warn("Employee service call failed: {}", cause.getMessage());
        return new IntegrationUnavailableException(message, cause);
    }
}
