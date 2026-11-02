package com.zodiakk.careerservice.security.access;

import com.zodiakk.careerservice.common.constants.SecurityConstants;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeAccessScopeDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.security.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("careerAccess")
@RequiredArgsConstructor
public class CareerAccessService {

    private final EmployeeDirectoryService employeeDirectory;

    public UUID currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    public UUID currentEmployeeId() {
        return employeeDirectory.findByUserId(currentUserId()).id();
    }

    public boolean isHrOrAdmin() {
        return SecurityUtils.hasAnyRole(
                SecurityConstants.HR_AUTHORITY,
                SecurityConstants.ADMIN_AUTHORITY
        );
    }

    public boolean isManagerOrAbove() {
        return SecurityUtils.hasAnyRole(
                SecurityConstants.MANAGER_AUTHORITY,
                SecurityConstants.HR_AUTHORITY,
                SecurityConstants.ADMIN_AUTHORITY
        );
    }

    public void requireHrOrAdmin() {
        if (!isHrOrAdmin()) {
            throw new AccessDeniedException("Недостаточно прав для управления карьерными справочниками");
        }
    }

    public void requireManagerOrAbove() {
        if (!isManagerOrAbove()) {
            throw new AccessDeniedException("Недостаточно прав для просмотра данных сотрудника");
        }
    }

    /**
     * Allows the acting employee themself and, additionally, HR/ADMIN and the employee's manager.
     */
    public void requireSelfOrManager(UUID employeeId) {
        if (isHrOrAdmin()) {
            return;
        }

        UUID actorEmployeeId = currentEmployeeId();

        if (actorEmployeeId.equals(employeeId)) {
            return;
        }

        EmployeeAccessScopeDto scope = employeeDirectory.getAccessScope(employeeId, actorEmployeeId);

        if (!scope.managerOfEmployee()) {
            throw new AccessDeniedException("Нет доступа к данным выбранного сотрудника");
        }
    }

    /**
     * Allows only the acting employee themself (HR/ADMIN are not impersonating in this flow).
     */
    public void requireSelfOrAdmin(UUID employeeId) {
        if (isHrOrAdmin()) {
            return;
        }

        if (!currentEmployeeId().equals(employeeId)) {
            throw new AccessDeniedException("Действие доступно только самому сотруднику");
        }
    }

    /**
     * Allows the employee's manager, HR and ADMIN. The employee themself is not eligible.
     */
    public void requireManagerOrHr(UUID employeeId) {
        if (isHrOrAdmin()) {
            return;
        }

        UUID actorEmployeeId = currentEmployeeId();

        EmployeeAccessScopeDto scope = employeeDirectory.getAccessScope(employeeId, actorEmployeeId);

        if (!scope.managerOfEmployee()) {
            throw new AccessDeniedException("Действие доступно руководителю сотрудника или HR");
        }
    }

    /**
     * Manager-of relationship check that never treats the employee as their own manager.
     */
    public boolean isManagerOf(UUID employeeId) {
        UUID actorEmployeeId = currentEmployeeId();

        if (actorEmployeeId.equals(employeeId)) {
            return false;
        }

        return employeeDirectory.getAccessScope(employeeId, actorEmployeeId).managerOfEmployee();
    }
}
