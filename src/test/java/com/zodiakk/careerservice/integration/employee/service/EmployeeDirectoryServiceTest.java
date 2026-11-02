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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDirectoryServiceTest {

    private static final UUID EMPLOYEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID POSITION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID GRADE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID SKILL_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    private EmployeeInternalClient client;

    @InjectMocks
    private EmployeeDirectoryService service;

    @Nested
    @DisplayName("findEmployees")
    class FindEmployees {

        @Test
        @DisplayName("returns an empty map without calling the client when no ids are requested")
        void shouldSkipEmptyLookup() {
            assertThat(service.findEmployees(List.of())).isEmpty();
            assertThat(service.findEmployees(null)).isEmpty();

            verifyNoInteractions(client);
        }

        @Test
        @DisplayName("deduplicates ids and indexes the returned employees")
        void shouldIndexReturnedEmployees() {
            EmployeeLookupItemDto employee = employee(true);
            ArgumentCaptor<EmployeeLookupRequestDto> captor =
                    ArgumentCaptor.forClass(EmployeeLookupRequestDto.class);
            when(client.lookupEmployees(captor.capture()))
                    .thenReturn(new EmployeeLookupResponseDto(List.of(employee)));

            assertThat(service.findEmployees(List.of(EMPLOYEE_ID, EMPLOYEE_ID)))
                    .containsEntry(EMPLOYEE_ID, employee);
            assertThat(captor.getValue().employeeIds()).containsExactly(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("returns an empty map when the client response has no employees")
        void shouldTolerateMissingEmployees() {
            when(client.lookupEmployees(any())).thenReturn(null);
            assertThat(service.findEmployees(List.of(EMPLOYEE_ID))).isEmpty();

            when(client.lookupEmployees(any())).thenReturn(new EmployeeLookupResponseDto(null));
            assertThat(service.findEmployees(List.of(EMPLOYEE_ID))).isEmpty();
        }

        @Test
        @DisplayName("translates a Feign failure into IntegrationUnavailableException")
        void shouldTranslateLookupFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.lookupEmployees(any())).thenThrow(failure);

            assertThatThrownBy(() -> service.findEmployees(List.of(EMPLOYEE_ID)))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage("Сервис сотрудников недоступен. Попробуйте позднее")
                    .hasCause(failure);
        }
    }

    @Nested
    @DisplayName("requireEmployee")
    class RequireEmployee {

        @Test
        @DisplayName("returns the employee when the lookup contains it")
        void shouldReturnEmployee() {
            EmployeeLookupItemDto employee = employee(true);
            when(client.lookupEmployees(any()))
                    .thenReturn(new EmployeeLookupResponseDto(List.of(employee)));

            assertThat(service.requireEmployee(EMPLOYEE_ID)).isSameAs(employee);
        }

        @Test
        @DisplayName("throws when the employee is missing from the lookup")
        void shouldRejectMissingEmployee() {
            when(client.lookupEmployees(any())).thenReturn(new EmployeeLookupResponseDto(List.of()));

            assertThatThrownBy(() -> service.requireEmployee(EMPLOYEE_ID))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Сотрудник с идентификатором " + EMPLOYEE_ID + " не найден");
        }
    }

    @Nested
    @DisplayName("reference lookup")
    class ReferenceLookup {

        @Test
        @DisplayName("returns an empty map without calling the client when no reference ids are requested")
        void shouldSkipEmptyReferenceLookup() {
            assertThat(service.findPositions(List.of())).isEmpty();
            assertThat(service.findPositions(null)).isEmpty();
            assertThat(service.findGrades(List.of())).isEmpty();
            assertThat(service.findGrades(null)).isEmpty();
            assertThat(service.findSkills(List.of())).isEmpty();
            assertThat(service.findSkills(null)).isEmpty();

            verifyNoInteractions(client);
        }

        @Test
        @DisplayName("indexes positions, grades and skills and deduplicates requested ids")
        void shouldIndexReferences() {
            ReferenceLookupItemDto position = reference(POSITION_ID, "Engineer");
            ReferenceLookupItemDto grade = reference(GRADE_ID, "Middle");
            ReferenceLookupItemDto skill = reference(SKILL_ID, "Java");
            ArgumentCaptor<ReferenceLookupRequestDto> captor =
                    ArgumentCaptor.forClass(ReferenceLookupRequestDto.class);

            when(client.lookupPositions(captor.capture()))
                    .thenReturn(new ReferenceLookupResponseDto(List.of(position)));
            when(client.lookupGrades(any()))
                    .thenReturn(new ReferenceLookupResponseDto(List.of(grade)));
            when(client.lookupSkills(any()))
                    .thenReturn(new ReferenceLookupResponseDto(List.of(skill)));

            assertThat(service.findPositions(List.of(POSITION_ID, POSITION_ID)))
                    .containsEntry(POSITION_ID, position);
            assertThat(captor.getValue().ids()).containsExactly(POSITION_ID);
            assertThat(service.findGrades(List.of(GRADE_ID))).containsEntry(GRADE_ID, grade);
            assertThat(service.findSkills(List.of(SKILL_ID))).containsEntry(SKILL_ID, skill);
        }

        @Test
        @DisplayName("returns an empty map when the reference response has no items")
        void shouldTolerateMissingReferenceItems() {
            when(client.lookupPositions(any())).thenReturn(null);
            assertThat(service.findPositions(List.of(POSITION_ID))).isEmpty();

            when(client.lookupPositions(any())).thenReturn(new ReferenceLookupResponseDto(null));
            assertThat(service.findPositions(List.of(POSITION_ID))).isEmpty();
        }

        @Test
        @DisplayName("translates a reference lookup failure into IntegrationUnavailableException")
        void shouldTranslateReferenceLookupFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.lookupSkills(any())).thenThrow(failure);

            assertThatThrownBy(() -> service.findSkills(List.of(SKILL_ID)))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage("Сервис сотрудников недоступен. Попробуйте позднее")
                    .hasCause(failure);
        }

        @Test
        @DisplayName("returns a position, grade or skill when the lookup contains it")
        void shouldRequireKnownReferences() {
            ReferenceLookupItemDto position = reference(POSITION_ID, "Engineer");
            ReferenceLookupItemDto grade = reference(GRADE_ID, "Middle");
            ReferenceLookupItemDto skill = reference(SKILL_ID, "Java");

            when(client.lookupPositions(any())).thenReturn(new ReferenceLookupResponseDto(List.of(position)));
            when(client.lookupGrades(any())).thenReturn(new ReferenceLookupResponseDto(List.of(grade)));
            when(client.lookupSkills(any())).thenReturn(new ReferenceLookupResponseDto(List.of(skill)));

            assertThat(service.requirePosition(POSITION_ID)).isSameAs(position);
            assertThat(service.requireGrade(GRADE_ID)).isSameAs(grade);
            assertThat(service.requireSkill(SKILL_ID)).isSameAs(skill);
        }

        @Test
        @DisplayName("throws a typed reference exception when a position, grade or skill is missing")
        void shouldRejectMissingReferences() {
            when(client.lookupPositions(any())).thenReturn(new ReferenceLookupResponseDto(List.of()));
            when(client.lookupGrades(any())).thenReturn(new ReferenceLookupResponseDto(List.of()));
            when(client.lookupSkills(any())).thenReturn(new ReferenceLookupResponseDto(List.of()));

            assertThatThrownBy(() -> service.requirePosition(POSITION_ID))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Должность с идентификатором " + POSITION_ID + " не найдена");
            assertThatThrownBy(() -> service.requireGrade(GRADE_ID))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Грейд с идентификатором " + GRADE_ID + " не найден");
            assertThatThrownBy(() -> service.requireSkill(SKILL_ID))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Навык с идентификатором " + SKILL_ID + " не найден");
        }
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("returns the employee linked to the user")
        void shouldReturnEmployeeByUserId() {
            EmployeeLookupItemDto employee = employee(true);
            when(client.getByUserId(USER_ID)).thenReturn(employee);

            assertThat(service.findByUserId(USER_ID)).isSameAs(employee);
        }

        @Test
        @DisplayName("translates a 404 into a missing-employee exception")
        void shouldTranslateNotFound() {
            when(client.getByUserId(USER_ID)).thenThrow(mock(FeignException.NotFound.class));

            assertThatThrownBy(() -> service.findByUserId(USER_ID))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Сотрудник с идентификатором " + USER_ID + " не найден");
        }

        @Test
        @DisplayName("translates other Feign failures into IntegrationUnavailableException")
        void shouldTranslateLookupFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.getByUserId(USER_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.findByUserId(USER_ID))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage("Сервис сотрудников недоступен. Попробуйте позднее")
                    .hasCause(failure);
        }
    }

    @Nested
    @DisplayName("getAccessScope")
    class GetAccessScope {

        @Test
        @DisplayName("returns the access scope from the employee service")
        void shouldReturnAccessScope() {
            EmployeeAccessScopeDto scope = new EmployeeAccessScopeDto(
                    EMPLOYEE_ID,
                    ACTOR_ID,
                    false,
                    true,
                    true
            );
            when(client.getAccessScope(EMPLOYEE_ID, ACTOR_ID)).thenReturn(scope);

            assertThat(service.getAccessScope(EMPLOYEE_ID, ACTOR_ID)).isSameAs(scope);
        }

        @Test
        @DisplayName("translates a 404 into a missing-employee exception")
        void shouldTranslateNotFound() {
            when(client.getAccessScope(EMPLOYEE_ID, ACTOR_ID))
                    .thenThrow(mock(FeignException.NotFound.class));

            assertThatThrownBy(() -> service.getAccessScope(EMPLOYEE_ID, ACTOR_ID))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Сотрудник с идентификатором " + EMPLOYEE_ID + " не найден");
        }

        @Test
        @DisplayName("translates other Feign failures into IntegrationUnavailableException")
        void shouldTranslateAccessScopeFailure() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(client.getAccessScope(EMPLOYEE_ID, ACTOR_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getAccessScope(EMPLOYEE_ID, ACTOR_ID))
                    .isInstanceOf(IntegrationUnavailableException.class)
                    .hasMessage("Не удалось получить область доступа к сотруднику")
                    .hasCause(failure);
        }
    }

    private EmployeeLookupItemDto employee(boolean active) {
        return new EmployeeLookupItemDto(EMPLOYEE_ID, "Ada Lovelace", active, POSITION_ID, GRADE_ID);
    }

    private ReferenceLookupItemDto reference(UUID id, String name) {
        return new ReferenceLookupItemDto(id, name, true);
    }
}
