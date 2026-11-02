package com.zodiakk.careerservice.security.access;

import com.zodiakk.careerservice.common.constants.SecurityConstants;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeAccessScopeDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.security.principal.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerAccessServiceTest {

    private static final UUID CURRENT_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID ACTOR_EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID EMPLOYEE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String EMAIL = "user@company.com";

    @Mock
    private EmployeeDirectoryService employeeDirectory;

    @InjectMocks
    private CareerAccessService careerAccessService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("currentUserId")
    class CurrentUserId {

        @Test
        @DisplayName("returns the authenticated user id")
        void shouldReturnCurrentUserId() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);

            assertThat(careerAccessService.currentUserId()).isEqualTo(CURRENT_USER_ID);
            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("throws when nobody is authenticated")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(() -> careerAccessService.currentUserId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");

            verifyNoInteractions(employeeDirectory);
        }
    }

    @Nested
    @DisplayName("currentEmployeeId")
    class CurrentEmployeeId {

        @Test
        @DisplayName("resolves the employee linked to the current user")
        void shouldReturnEmployeeId() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());

            assertThat(careerAccessService.currentEmployeeId()).isEqualTo(ACTOR_EMPLOYEE_ID);
        }

        @Test
        @DisplayName("throws when nobody is authenticated")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(() -> careerAccessService.currentEmployeeId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");

            verifyNoInteractions(employeeDirectory);
        }
    }

    @Nested
    @DisplayName("isHrOrAdmin")
    class IsHrOrAdmin {

        @Test
        @DisplayName("is true for HR")
        void shouldBeTrueForHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            assertThat(careerAccessService.isHrOrAdmin()).isTrue();
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            assertThat(careerAccessService.isHrOrAdmin()).isTrue();
        }

        @Test
        @DisplayName("is false for MANAGER")
        void shouldBeFalseForManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);

            assertThat(careerAccessService.isHrOrAdmin()).isFalse();
        }

        @Test
        @DisplayName("is false for EMPLOYEE")
        void shouldBeFalseForEmployee() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);

            assertThat(careerAccessService.isHrOrAdmin()).isFalse();
        }

        @Test
        @DisplayName("is false when the current user has no role")
        void shouldBeFalseWhenRoleIsNull() {
            authenticate(null);

            assertThat(careerAccessService.isHrOrAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("isManagerOrAbove")
    class IsManagerOrAbove {

        @Test
        @DisplayName("is true for MANAGER")
        void shouldBeTrueForManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);

            assertThat(careerAccessService.isManagerOrAbove()).isTrue();
        }

        @Test
        @DisplayName("is true for HR")
        void shouldBeTrueForHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            assertThat(careerAccessService.isManagerOrAbove()).isTrue();
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            assertThat(careerAccessService.isManagerOrAbove()).isTrue();
        }

        @Test
        @DisplayName("is false for EMPLOYEE")
        void shouldBeFalseForEmployee() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);

            assertThat(careerAccessService.isManagerOrAbove()).isFalse();
        }

        @Test
        @DisplayName("is false when the current user has no role")
        void shouldBeFalseWhenRoleIsNull() {
            authenticate(null);

            assertThat(careerAccessService.isManagerOrAbove()).isFalse();
        }
    }

    @Nested
    @DisplayName("requireHrOrAdmin")
    class RequireHrOrAdmin {

        @Test
        @DisplayName("allows HR")
        void shouldAllowHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            careerAccessService.requireHrOrAdmin();

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows ADMIN")
        void shouldAllowAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            careerAccessService.requireHrOrAdmin();

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("denies MANAGER")
        void shouldDenyManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);

            assertThatThrownBy(() -> careerAccessService.requireHrOrAdmin())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Недостаточно прав для управления карьерными справочниками");
        }

        @Test
        @DisplayName("denies EMPLOYEE")
        void shouldDenyEmployee() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);

            assertThatThrownBy(() -> careerAccessService.requireHrOrAdmin())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Недостаточно прав для управления карьерными справочниками");
        }

        @Test
        @DisplayName("throws when nobody is authenticated")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(() -> careerAccessService.requireHrOrAdmin())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
        }
    }

    @Nested
    @DisplayName("requireManagerOrAbove")
    class RequireManagerOrAbove {

        @Test
        @DisplayName("allows MANAGER")
        void shouldAllowManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);

            careerAccessService.requireManagerOrAbove();
        }

        @Test
        @DisplayName("allows HR")
        void shouldAllowHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            careerAccessService.requireManagerOrAbove();
        }

        @Test
        @DisplayName("allows ADMIN")
        void shouldAllowAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            careerAccessService.requireManagerOrAbove();
        }

        @Test
        @DisplayName("denies EMPLOYEE")
        void shouldDenyEmployee() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);

            assertThatThrownBy(() -> careerAccessService.requireManagerOrAbove())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Недостаточно прав для просмотра данных сотрудника");
        }
    }

    @Nested
    @DisplayName("requireSelfOrManager")
    class RequireSelfOrManager {

        @Test
        @DisplayName("allows HR without resolving the actor")
        void shouldAllowHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            careerAccessService.requireSelfOrManager(EMPLOYEE_ID);

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows ADMIN without resolving the actor")
        void shouldAllowAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            careerAccessService.requireSelfOrManager(EMPLOYEE_ID);

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows the employee themself")
        void shouldAllowSelf() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID))
                    .thenReturn(employee(EMPLOYEE_ID));

            careerAccessService.requireSelfOrManager(EMPLOYEE_ID);

            verify(employeeDirectory).findByUserId(CURRENT_USER_ID);
            verifyNoScopeLookup();
        }

        @Test
        @DisplayName("allows the employee's manager")
        void shouldAllowManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, true));

            careerAccessService.requireSelfOrManager(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("denies an employee who is neither self nor the manager")
        void shouldDenyForeignEmployee() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, false));

            assertThatThrownBy(() -> careerAccessService.requireSelfOrManager(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Нет доступа к данным выбранного сотрудника");
        }

        @Test
        @DisplayName("denies a manager who does not manage the employee")
        void shouldDenyUnrelatedManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, false));

            assertThatThrownBy(() -> careerAccessService.requireSelfOrManager(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Нет доступа к данным выбранного сотрудника");
        }
    }

    @Nested
    @DisplayName("requireSelfOrAdmin")
    class RequireSelfOrAdmin {

        @Test
        @DisplayName("allows HR without resolving the actor")
        void shouldAllowHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            careerAccessService.requireSelfOrAdmin(EMPLOYEE_ID);

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows ADMIN without resolving the actor")
        void shouldAllowAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            careerAccessService.requireSelfOrAdmin(EMPLOYEE_ID);

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows the employee themself")
        void shouldAllowSelf() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID))
                    .thenReturn(employee(EMPLOYEE_ID));

            careerAccessService.requireSelfOrAdmin(EMPLOYEE_ID);

            verifyNoScopeLookup();
        }

        @Test
        @DisplayName("denies a manager of the employee")
        void shouldDenyManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());

            assertThatThrownBy(() -> careerAccessService.requireSelfOrAdmin(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Действие доступно только самому сотруднику");

            verifyNoScopeLookup();
        }

        @Test
        @DisplayName("denies another employee")
        void shouldDenyForeignEmployee() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());

            assertThatThrownBy(() -> careerAccessService.requireSelfOrAdmin(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Действие доступно только самому сотруднику");
        }
    }

    @Nested
    @DisplayName("requireManagerOrHr")
    class RequireManagerOrHr {

        @Test
        @DisplayName("allows HR without resolving the actor")
        void shouldAllowHr() {
            authenticate(SecurityConstants.HR_AUTHORITY);

            careerAccessService.requireManagerOrHr(EMPLOYEE_ID);

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows ADMIN without resolving the actor")
        void shouldAllowAdmin() {
            authenticate(SecurityConstants.ADMIN_AUTHORITY);

            careerAccessService.requireManagerOrHr(EMPLOYEE_ID);

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("allows the employee's manager")
        void shouldAllowManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, true));

            careerAccessService.requireManagerOrHr(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("denies the employee themself")
        void shouldDenySelf() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID))
                    .thenReturn(employee(EMPLOYEE_ID));
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, EMPLOYEE_ID))
                    .thenReturn(scope(true, false));

            assertThatThrownBy(() -> careerAccessService.requireManagerOrHr(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Действие доступно руководителю сотрудника или HR");
        }

        @Test
        @DisplayName("denies a manager who does not manage the employee")
        void shouldDenyUnrelatedManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, false));

            assertThatThrownBy(() -> careerAccessService.requireManagerOrHr(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Действие доступно руководителю сотрудника или HR");
        }
    }

    @Nested
    @DisplayName("isManagerOf")
    class IsManagerOf {

        @Test
        @DisplayName("is false for the employee themself and does not ask for a scope")
        void shouldBeFalseForSelf() {
            authenticate(SecurityConstants.EMPLOYEE_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID))
                    .thenReturn(employee(EMPLOYEE_ID));

            assertThat(careerAccessService.isManagerOf(EMPLOYEE_ID)).isFalse();
            verifyNoScopeLookup();
        }

        @Test
        @DisplayName("is true when the access scope marks the actor as manager")
        void shouldBeTrueForManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, true));

            assertThat(careerAccessService.isManagerOf(EMPLOYEE_ID)).isTrue();
        }

        @Test
        @DisplayName("is false when the access scope does not mark the actor as manager")
        void shouldBeFalseWhenNotManager() {
            authenticate(SecurityConstants.MANAGER_AUTHORITY);
            when(employeeDirectory.findByUserId(CURRENT_USER_ID)).thenReturn(actor());
            when(employeeDirectory.getAccessScope(EMPLOYEE_ID, ACTOR_EMPLOYEE_ID))
                    .thenReturn(scope(false, false));

            assertThat(careerAccessService.isManagerOf(EMPLOYEE_ID)).isFalse();
        }

        @Test
        @DisplayName("throws when nobody is authenticated")
        void shouldThrowWithoutAuthentication() {
            assertThatThrownBy(() -> careerAccessService.isManagerOf(EMPLOYEE_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");

            verifyNoInteractions(employeeDirectory);
        }
    }

    private void verifyNoScopeLookup() {
        verify(employeeDirectory, never()).getAccessScope(any(), any());
    }

    private void authenticate(String role) {
        CurrentUser currentUser = CurrentUser.builder()
                .userId(CURRENT_USER_ID)
                .role(role)
                .email(EMAIL)
                .build();
        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(role));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities)
        );
    }

    private EmployeeLookupItemDto actor() {
        return employee(ACTOR_EMPLOYEE_ID);
    }

    private EmployeeLookupItemDto employee(UUID employeeId) {
        return new EmployeeLookupItemDto(employeeId, "Actor", true, null, null);
    }

    private EmployeeAccessScopeDto scope(boolean self, boolean managerOfEmployee) {
        return new EmployeeAccessScopeDto(
                EMPLOYEE_ID,
                ACTOR_EMPLOYEE_ID,
                self,
                managerOfEmployee,
                true
        );
    }
}
