package com.zodiakk.careerservice.career.service;

import com.zodiakk.careerservice.career.dto.request.action.CareerGoalCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerGoalTransitionRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.EmployeeCareerGoalFilterDto;
import com.zodiakk.careerservice.career.dto.request.update.EmployeeCareerGoalUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.EmployeeCareerGoalResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTrack;
import com.zodiakk.careerservice.career.entity.CareerTrackNode;
import com.zodiakk.careerservice.career.entity.EmployeeCareerGoal;
import com.zodiakk.careerservice.career.mapper.EmployeeCareerGoalMapper;
import com.zodiakk.careerservice.career.repository.CareerTrackNodeRepository;
import com.zodiakk.careerservice.career.repository.CareerTrackRepository;
import com.zodiakk.careerservice.career.repository.EmployeeCareerGoalRepository;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.constants.SecurityConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import com.zodiakk.careerservice.common.enums.CareerGoalStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackType;
import com.zodiakk.careerservice.common.exception.domain.ActiveCareerGoalExistsException;
import com.zodiakk.careerservice.common.exception.domain.CareerGoalNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.CareerTrackStructureNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
import com.zodiakk.careerservice.security.principal.CurrentUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerGoalServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("23232323-2323-2323-2323-232323232323");

    private static final UUID GOAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID OTHER_GOAL_ID = UUID.fromString("34343434-3434-3434-3434-343434343434");

    private static final UUID POSITION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID OTHER_POSITION_ID = UUID.fromString("45454545-4545-4545-4545-454545454545");

    private static final UUID GRADE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID OTHER_GRADE_ID = UUID.fromString("56565656-5656-5656-5656-565656565656");

    private static final UUID NODE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final UUID TRACK_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final Long VERSION = 7L;

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 12, 1);

    @Mock
    private EmployeeCareerGoalRepository goalRepository;

    @Mock
    private CareerTrackNodeRepository nodeRepository;

    @Mock
    private CareerTrackRepository trackRepository;

    @Mock
    private EmployeeCareerGoalMapper goalMapper;

    @Mock
    private EmployeeDirectoryService employeeDirectory;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private CareerAccessService accessService;

    @InjectMocks
    private CareerGoalService careerGoalService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("listByEmployee")
    class ListByEmployee {

        @Test
        @DisplayName("forces the employee id and resolves position and grade names")
        void shouldListGoalsForEmployee() {
            EmployeeCareerGoalFilterDto filter = EmployeeCareerGoalFilterDto.builder()
                    .employeeId(OTHER_EMPLOYEE_ID)
                    .status(CareerGoalStatus.PROPOSED)
                    .build();
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            PageRequestDto pageRequest = PageRequestDto.builder()
                    .page(0)
                    .size(10)
                    .sortBy("targetDate")
                    .sortDirection(SortDirection.DESC)
                    .build();
            when(goalRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(goal), PageRequest.of(0, 10), 1));
            EmployeeCareerGoalResponseDto response = stubNamedResponse();

            Page<EmployeeCareerGoalResponseDto> result =
                    careerGoalService.listByEmployee(EMPLOYEE_ID, filter, pageRequest);

            assertThat(filter.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(filter.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            assertThat(result.getContent()).containsExactly(response);
            assertThat(response.getTargetPositionName()).isEqualTo("Engineer");
            assertThat(response.getTargetGradeName()).isEqualTo("Senior");
            assertThat(response.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            verify(accessService).requireSelfOrManager(EMPLOYEE_ID);
            verify(employeeDirectory).findPositions(List.of(POSITION_ID));
            verify(employeeDirectory).findGrades(List.of(GRADE_ID));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            ArgumentCaptor<Specification<EmployeeCareerGoal>> specificationCaptor =
                    ArgumentCaptor.forClass(Specification.class);
            verify(goalRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(10);
            assertThat(pageable.getSort().getOrderFor("targetDate").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertSpecificationEqualsEmployee(specificationCaptor.getValue());
        }

        @Test
        @DisplayName("scopes a missing filter to the requested employee")
        void shouldScopeMissingFilterToEmployee() {
            when(goalRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(careerGoalService.listByEmployee(EMPLOYEE_ID, null, pageRequest())).isEmpty();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Specification<EmployeeCareerGoal>> specificationCaptor =
                    ArgumentCaptor.forClass(Specification.class);
            verify(goalRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
            assertSpecificationEqualsEmployee(specificationCaptor.getValue());
            verify(employeeDirectory).findPositions(List.of());
            verify(employeeDirectory).findGrades(List.of());
            verifyNoInteractions(goalMapper);
        }

        @Test
        @DisplayName("returns an empty page when the employee has no goals")
        void shouldReturnEmptyPage() {
            EmployeeCareerGoalFilterDto filter = EmployeeCareerGoalFilterDto.builder()
                    .status(CareerGoalStatus.ACTIVE)
                    .build();
            when(goalRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            Page<EmployeeCareerGoalResponseDto> result =
                    careerGoalService.listByEmployee(EMPLOYEE_ID, filter, pageRequest());

            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(filter.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            verifyNoInteractions(goalMapper);
        }

        @Test
        @DisplayName("denies a user who cannot see the employee")
        void shouldDenyWithoutAccess() {
            deny(EMPLOYEE_ID);

            assertThatThrownBy(() -> careerGoalService.listByEmployee(EMPLOYEE_ID, null, pageRequest()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(goalRepository, employeeDirectory);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("proposes a goal without a track node")
        void shouldCreateProposedGoal() {
            authenticate();
            CareerGoalCreateRequestDto request = createRequest(null);
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(activeEmployee());
            when(goalRepository.save(any(EmployeeCareerGoal.class))).thenAnswer(invocation -> {
                EmployeeCareerGoal saved = invocation.getArgument(0);
                saved.setId(GOAL_ID);
                return saved;
            });
            EmployeeCareerGoalResponseDto response = stubNamedResponse();

            EmployeeCareerGoalResponseDto result = careerGoalService.create(EMPLOYEE_ID, request);

            assertThat(result).isSameAs(response);
            assertThat(result.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            assertThat(result.getTargetPositionName()).isEqualTo("Engineer");
            assertThat(result.getTargetGradeName()).isEqualTo("Senior");

            ArgumentCaptor<EmployeeCareerGoal> captor = ArgumentCaptor.forClass(EmployeeCareerGoal.class);
            verify(goalRepository).save(captor.capture());
            EmployeeCareerGoal saved = captor.getValue();
            assertThat(saved.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(saved.getTargetPositionId()).isEqualTo(POSITION_ID);
            assertThat(saved.getTargetGradeId()).isEqualTo(GRADE_ID);
            assertThat(saved.getTargetNodeId()).isNull();
            assertThat(saved.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            assertThat(saved.getMotivation()).isEqualTo("Grow");
            assertThat(saved.getTargetDate()).isEqualTo(TARGET_DATE);
            assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
            verify(employeeDirectory).requirePosition(POSITION_ID);
            verify(employeeDirectory).requireGrade(GRADE_ID);
            verifyNoInteractions(nodeRepository, trackRepository);
            verify(goalRepository, never()).findFirstByEmployeeIdAndStatus(any(), any());
            verifyChangedEvent(CareerGoalStatus.PROPOSED);
        }

        @Test
        @DisplayName("accepts a node on a published track when position and grade match")
        void shouldCreateGoalForPublishedNode() {
            authenticate();
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(activeEmployee());
            stubPublishedNode(POSITION_ID, GRADE_ID);
            when(goalRepository.save(any(EmployeeCareerGoal.class))).thenAnswer(invocation -> {
                EmployeeCareerGoal saved = invocation.getArgument(0);
                saved.setId(GOAL_ID);
                return saved;
            });
            stubNamedResponse();

            careerGoalService.create(EMPLOYEE_ID, createRequest(NODE_ID));

            ArgumentCaptor<EmployeeCareerGoal> captor = ArgumentCaptor.forClass(EmployeeCareerGoal.class);
            verify(goalRepository).save(captor.capture());
            assertThat(captor.getValue().getTargetNodeId()).isEqualTo(NODE_ID);
        }

        @Test
        @DisplayName("rejects an inactive employee before checking the position")
        void shouldRejectInactiveEmployee() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID))
                    .thenReturn(new EmployeeLookupItemDto(EMPLOYEE_ID, "Ada", false, POSITION_ID, GRADE_ID));

            assertThatThrownBy(() -> careerGoalService.create(EMPLOYEE_ID, createRequest(null)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Неактивному сотруднику нельзя создать карьерную цель");

            verify(employeeDirectory, never()).requirePosition(any());
            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the target node does not exist")
        void shouldThrowWhenNodeMissing() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(activeEmployee());
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerGoalService.create(EMPLOYEE_ID, createRequest(NODE_ID)))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(NODE_ID.toString());

            verifyNoInteractions(trackRepository);
            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a node that does not match the position and grade")
        void shouldRejectMismatchedNode() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(activeEmployee());
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(OTHER_POSITION_ID, GRADE_ID)));

            assertThatThrownBy(() -> careerGoalService.create(EMPLOYEE_ID, createRequest(NODE_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Целевой узел не соответствует выбранной должности и грейду");

            verifyNoInteractions(trackRepository);
        }

        @Test
        @DisplayName("rejects a node whose track does not exist")
        void shouldRejectNodeWithoutTrack() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(activeEmployee());
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(POSITION_ID, GRADE_ID)));
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerGoalService.create(EMPLOYEE_ID, createRequest(NODE_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Целевой узел принадлежит неопубликованному карьерному треку");
        }

        @ParameterizedTest
        @EnumSource(value = CareerTrackStatus.class, names = {"DRAFT", "ARCHIVED"})
        @DisplayName("rejects a node on an unpublished track")
        void shouldRejectUnpublishedTrack(CareerTrackStatus status) {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(activeEmployee());
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(POSITION_ID, GRADE_ID)));
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track(status)));

            assertThatThrownBy(() -> careerGoalService.create(EMPLOYEE_ID, createRequest(NODE_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Целевой узел принадлежит неопубликованному карьерному треку");

            verify(goalRepository, never()).save(any());
        }

        @Test
        @DisplayName("denies a user who cannot act for the employee")
        void shouldDenyWithoutAccess() {
            deny(EMPLOYEE_ID);

            assertThatThrownBy(() -> careerGoalService.create(EMPLOYEE_ID, createRequest(null)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(employeeDirectory, goalRepository);
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns the goal and leaves names empty when reference data is missing")
        void shouldReturnGoalWithMissingNames() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal));
            EmployeeCareerGoalResponseDto response = EmployeeCareerGoalResponseDto.builder().id(GOAL_ID).build();
            when(goalMapper.toResponseDto(goal)).thenReturn(response);
            when(employeeDirectory.findPositions(List.of(POSITION_ID))).thenReturn(Map.of());
            when(employeeDirectory.findGrades(List.of(GRADE_ID))).thenReturn(Map.of());

            EmployeeCareerGoalResponseDto result = careerGoalService.get(GOAL_ID);

            assertThat(result).isSameAs(response);
            assertThat(result.getTargetPositionName()).isNull();
            assertThat(result.getTargetGradeName()).isNull();
            verify(accessService).requireSelfOrManager(EMPLOYEE_ID);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("throws when the goal does not exist")
        void shouldThrowWhenMissing() {
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerGoalService.get(GOAL_ID))
                    .isInstanceOf(CareerGoalNotFoundException.class)
                    .hasMessageContaining(GOAL_ID.toString());

            verify(accessService, never()).requireSelfOrManager(any());
            verifyNoInteractions(goalMapper);
        }

        @Test
        @DisplayName("denies a user who cannot see the goal owner")
        void shouldDenyWithoutAccess() {
            when(goalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal(CareerGoalStatus.PROPOSED)));
            deny(EMPLOYEE_ID);

            assertThatThrownBy(() -> careerGoalService.get(GOAL_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(goalMapper, employeeDirectory);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("keeps the current status when the mapper tries to change it")
        void shouldKeepStatusWhileUpdatingMotivation() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            EmployeeCareerGoalUpdateRequestDto request = EmployeeCareerGoalUpdateRequestDto.builder()
                    .motivation("Updated")
                    .targetDate(TARGET_DATE.plusDays(1))
                    .status(CareerGoalStatus.ACHIEVED)
                    .build();
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            applyGoalUpdate();
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            EmployeeCareerGoalResponseDto result = careerGoalService.update(GOAL_ID, request);

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            assertThat(goal.getMotivation()).isEqualTo("Updated");
            assertThat(goal.getTargetDate()).isEqualTo(TARGET_DATE.plusDays(1));
            assertThat(result.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            verify(employeeDirectory, never()).requirePosition(any());
            verify(employeeDirectory, never()).requireGrade(any());
            verifyNoInteractions(nodeRepository, trackRepository);
            verifyChangedEvent(CareerGoalStatus.PROPOSED);
        }

        @Test
        @DisplayName("updates an active goal")
        void shouldUpdateActiveGoal() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.ACTIVE);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            applyGoalUpdate();
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder().motivation("Still going").build()
            );

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.ACTIVE);
            assertThat(goal.getMotivation()).isEqualTo("Still going");
        }

        @Test
        @DisplayName("checks a new position and skips node validation when the node is omitted")
        void shouldUpdatePositionWithoutNode() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            applyGoalUpdate();
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder().targetPositionId(OTHER_POSITION_ID).build()
            );

            assertThat(goal.getTargetPositionId()).isEqualTo(OTHER_POSITION_ID);
            assertThat(goal.getTargetGradeId()).isEqualTo(GRADE_ID);
            verify(employeeDirectory).requirePosition(OTHER_POSITION_ID);
            verify(employeeDirectory, never()).requireGrade(any());
            verifyNoInteractions(nodeRepository, trackRepository);
        }

        @Test
        @DisplayName("accepts a new grade when the node matches the published track")
        void shouldUpdateGradeWithPublishedNode() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            stubPublishedNode(POSITION_ID, OTHER_GRADE_ID);
            applyGoalUpdate();
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder()
                            .targetGradeId(OTHER_GRADE_ID)
                            .targetNodeId(NODE_ID)
                            .build()
            );

            assertThat(goal.getTargetGradeId()).isEqualTo(OTHER_GRADE_ID);
            assertThat(goal.getTargetNodeId()).isEqualTo(NODE_ID);
            verify(employeeDirectory).requireGrade(OTHER_GRADE_ID);
            verify(employeeDirectory, never()).requirePosition(any());
        }

        @Test
        @DisplayName("rejects a node that does not match the new position")
        void shouldRejectMismatchedNode() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(POSITION_ID, GRADE_ID)));

            assertThatThrownBy(() -> careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder()
                            .targetPositionId(OTHER_POSITION_ID)
                            .targetNodeId(NODE_ID)
                            .build()
            ))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Целевой узел не соответствует выбранной должности и грейду");

            verify(goalRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @EnumSource(value = CareerGoalStatus.class, names = {"ACHIEVED", "CANCELLED"})
        @DisplayName("rejects changes to a terminal goal")
        void shouldRejectTerminalGoal(CareerGoalStatus status) {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal(status)));

            assertThatThrownBy(() -> careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder().motivation("Too late").build()
            ))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(status.name())
                    .hasMessageContaining("изменение");

            verifyNoInteractions(goalMapper, outboxEventService);
            verify(goalRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("throws when the goal does not exist")
        void shouldThrowWhenMissing() {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder().motivation("Updated").build()
            ))
                    .isInstanceOf(CareerGoalNotFoundException.class)
                    .hasMessageContaining(GOAL_ID.toString());

            verify(accessService, never()).requireSelfOrManager(any());
        }

        @Test
        @DisplayName("denies a user who cannot change the goal")
        void shouldDenyWithoutAccess() {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal(CareerGoalStatus.PROPOSED)));
            deny(EMPLOYEE_ID);

            assertThatThrownBy(() -> careerGoalService.update(
                    GOAL_ID,
                    EmployeeCareerGoalUpdateRequestDto.builder().motivation("Updated").build()
            )).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(goalMapper);
            verify(goalRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("moves PROPOSED to ACTIVE when the employee has no other active goal")
        void shouldActivateProposedGoal() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.findFirstByEmployeeIdAndStatus(EMPLOYEE_ID, CareerGoalStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            EmployeeCareerGoalResponseDto result = careerGoalService.activate(GOAL_ID, transition(VERSION));

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.ACTIVE);
            assertThat(result.getStatus()).isEqualTo(CareerGoalStatus.ACTIVE);
            verifyChangedEvent(CareerGoalStatus.ACTIVE);
        }

        @Test
        @DisplayName("activates when the only active row is the goal itself")
        void shouldIgnoreTheSameGoalWhenCheckingTheActiveGoal() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.findFirstByEmployeeIdAndStatus(EMPLOYEE_ID, CareerGoalStatus.ACTIVE))
                    .thenReturn(Optional.of(goal));
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            careerGoalService.activate(GOAL_ID, null);

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.ACTIVE);
        }

        @Test
        @DisplayName("rejects activation when another goal is already active")
        void shouldRejectWhenAnotherGoalIsActive() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            EmployeeCareerGoal other = goal(CareerGoalStatus.ACTIVE);
            other.setId(OTHER_GOAL_ID);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.findFirstByEmployeeIdAndStatus(EMPLOYEE_ID, CareerGoalStatus.ACTIVE))
                    .thenReturn(Optional.of(other));

            assertThatThrownBy(() -> careerGoalService.activate(GOAL_ID, transition(null)))
                    .isInstanceOf(ActiveCareerGoalExistsException.class)
                    .hasMessageContaining(EMPLOYEE_ID.toString());

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            verify(goalRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxEventService);
        }

        @ParameterizedTest
        @EnumSource(value = CareerGoalStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PROPOSED")
        @DisplayName("rejects activation from any status other than PROPOSED")
        void shouldRejectIllegalSource(CareerGoalStatus status) {
            EmployeeCareerGoal goal = goal(status);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));

            assertThatThrownBy(() -> careerGoalService.activate(GOAL_ID, transition(null)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(status.name())
                    .hasMessageContaining(CareerGoalStatus.ACTIVE.name());

            verify(goalRepository, never()).findFirstByEmployeeIdAndStatus(any(), any());
            verify(goalRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("rejects a stale expected version before changing the status")
        void shouldRejectStaleVersion() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.PROPOSED);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));

            assertThatThrownBy(() -> careerGoalService.activate(GOAL_ID, transition(VERSION + 1)))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessage("Ожидаемая версия карьерной цели не совпадает с текущей");

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.PROPOSED);
            verify(goalRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("throws when the goal does not exist")
        void shouldThrowWhenMissing() {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerGoalService.activate(GOAL_ID, transition(VERSION)))
                    .isInstanceOf(CareerGoalNotFoundException.class)
                    .hasMessageContaining(GOAL_ID.toString());

            verify(accessService, never()).requireSelfOrManager(any());
        }

        @Test
        @DisplayName("denies a user who cannot change the goal")
        void shouldDenyWithoutAccess() {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal(CareerGoalStatus.PROPOSED)));
            deny(EMPLOYEE_ID);

            assertThatThrownBy(() -> careerGoalService.activate(GOAL_ID, transition(VERSION)))
                    .isInstanceOf(AccessDeniedException.class);

            verify(goalRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("achieve")
    class Achieve {

        @Test
        @DisplayName("moves ACTIVE to ACHIEVED without checking other active goals")
        void shouldAchieveActiveGoal() {
            EmployeeCareerGoal goal = goal(CareerGoalStatus.ACTIVE);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            EmployeeCareerGoalResponseDto result = careerGoalService.achieve(GOAL_ID, transition(null));

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.ACHIEVED);
            assertThat(result.getStatus()).isEqualTo(CareerGoalStatus.ACHIEVED);
            verify(goalRepository, never()).findFirstByEmployeeIdAndStatus(any(), any());
            verifyChangedEvent(CareerGoalStatus.ACHIEVED);
        }

        @ParameterizedTest
        @EnumSource(value = CareerGoalStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
        @DisplayName("rejects achievement from any status other than ACTIVE")
        void shouldRejectIllegalSource(CareerGoalStatus status) {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal(status)));

            assertThatThrownBy(() -> careerGoalService.achieve(GOAL_ID, null))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(status.name())
                    .hasMessageContaining(CareerGoalStatus.ACHIEVED.name());

            verify(goalRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @ParameterizedTest
        @EnumSource(value = CareerGoalStatus.class, names = {"PROPOSED", "ACTIVE"})
        @DisplayName("cancels a proposed or active goal")
        void shouldCancelOpenGoal(CareerGoalStatus status) {
            EmployeeCareerGoal goal = goal(status);
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal));
            when(goalRepository.saveAndFlush(goal)).thenReturn(goal);
            stubNamedResponse();

            EmployeeCareerGoalResponseDto result = careerGoalService.cancel(GOAL_ID, transition(VERSION));

            assertThat(goal.getStatus()).isEqualTo(CareerGoalStatus.CANCELLED);
            assertThat(result.getStatus()).isEqualTo(CareerGoalStatus.CANCELLED);
            verify(goalRepository, never()).findFirstByEmployeeIdAndStatus(any(), any());
            verifyChangedEvent(CareerGoalStatus.CANCELLED);
        }

        @ParameterizedTest
        @EnumSource(value = CareerGoalStatus.class, names = {"ACHIEVED", "CANCELLED"})
        @DisplayName("rejects cancellation of a terminal goal")
        void shouldRejectTerminalGoal(CareerGoalStatus status) {
            when(goalRepository.findByIdForUpdate(GOAL_ID)).thenReturn(Optional.of(goal(status)));

            assertThatThrownBy(() -> careerGoalService.cancel(GOAL_ID, transition(null)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(status.name())
                    .hasMessageContaining(CareerGoalStatus.CANCELLED.name());

            verify(goalRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxEventService);
        }
    }

    private void verifyChangedEvent(CareerGoalStatus status) {
        verify(outboxEventService).saveEvent(
                eq(GOAL_ID),
                eq(KafkaEventConstants.AGGREGATE_TYPE_CAREER_GOAL),
                eq(KafkaEventConstants.CAREER_GOAL_CHANGED),
                eq(KafkaEventConstants.EVENT_VERSION_V1),
                eq(Map.of(
                        "goalId", GOAL_ID,
                        "employeeId", EMPLOYEE_ID,
                        "targetPositionId", POSITION_ID,
                        "targetGradeId", GRADE_ID,
                        "status", status.name()
                ))
        );
    }

    private EmployeeCareerGoalResponseDto stubNamedResponse() {
        EmployeeCareerGoalResponseDto response = EmployeeCareerGoalResponseDto.builder().id(GOAL_ID).build();
        when(goalMapper.toResponseDto(any(EmployeeCareerGoal.class))).thenAnswer(invocation -> {
            EmployeeCareerGoal goal = invocation.getArgument(0);
            response.setId(goal.getId());
            response.setEmployeeId(goal.getEmployeeId());
            response.setStatus(goal.getStatus());
            return response;
        });
        when(employeeDirectory.findPositions(any())).thenReturn(Map.of(
                POSITION_ID, new ReferenceLookupItemDto(POSITION_ID, "Engineer", true)
        ));
        when(employeeDirectory.findGrades(any())).thenReturn(Map.of(
                GRADE_ID, new ReferenceLookupItemDto(GRADE_ID, "Senior", true)
        ));
        return response;
    }

    private void applyGoalUpdate() {
        doAnswer(invocation -> {
            EmployeeCareerGoalUpdateRequestDto request = invocation.getArgument(0);
            EmployeeCareerGoal goal = invocation.getArgument(1);
            if (request.getTargetPositionId() != null) {
                goal.setTargetPositionId(request.getTargetPositionId());
            }
            if (request.getTargetGradeId() != null) {
                goal.setTargetGradeId(request.getTargetGradeId());
            }
            if (request.getTargetNodeId() != null) {
                goal.setTargetNodeId(request.getTargetNodeId());
            }
            if (request.getStatus() != null) {
                goal.setStatus(request.getStatus());
            }
            if (request.getMotivation() != null) {
                goal.setMotivation(request.getMotivation());
            }
            if (request.getTargetDate() != null) {
                goal.setTargetDate(request.getTargetDate());
            }
            return null;
        }).when(goalMapper).updateEntity(
                any(EmployeeCareerGoalUpdateRequestDto.class),
                any(EmployeeCareerGoal.class)
        );
    }

    private void stubPublishedNode(UUID positionId, UUID gradeId) {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(positionId, gradeId)));
        when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));
    }

    @SuppressWarnings("unchecked")
    private void assertSpecificationEqualsEmployee(Specification<EmployeeCareerGoal> specification) {
        Root<EmployeeCareerGoal> root = mock(Root.class);
        CriteriaQuery<EmployeeCareerGoal> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> employeePath = mock(Path.class);
        when(root.get("employeeId")).thenReturn(employeePath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(employeePath, EMPLOYEE_ID);
    }

    private EmployeeCareerGoal goal(CareerGoalStatus status) {
        return EmployeeCareerGoal.builder()
                .id(GOAL_ID)
                .employeeId(EMPLOYEE_ID)
                .targetPositionId(POSITION_ID)
                .targetGradeId(GRADE_ID)
                .status(status)
                .motivation("Grow")
                .targetDate(TARGET_DATE)
                .version(VERSION)
                .createdBy(USER_ID)
                .build();
    }

    private CareerTrackNode node(UUID positionId, UUID gradeId) {
        return CareerTrackNode.builder()
                .id(NODE_ID)
                .trackId(TRACK_ID)
                .positionId(positionId)
                .gradeId(gradeId)
                .displayOrder(1)
                .isEntryPoint(false)
                .build();
    }

    private CareerTrack track(CareerTrackStatus status) {
        return CareerTrack.builder()
                .id(TRACK_ID)
                .code("ENG")
                .name("Backend")
                .type(CareerTrackType.VERTICAL)
                .status(status)
                .build();
    }

    private CareerGoalCreateRequestDto createRequest(UUID nodeId) {
        return CareerGoalCreateRequestDto.builder()
                .targetPositionId(POSITION_ID)
                .targetGradeId(GRADE_ID)
                .targetNodeId(nodeId)
                .targetDate(TARGET_DATE)
                .motivation("Grow")
                .build();
    }

    private CareerGoalTransitionRequestDto transition(Long expectedVersion) {
        return CareerGoalTransitionRequestDto.builder()
                .comment("Ready")
                .expectedVersion(expectedVersion)
                .build();
    }

    private PageRequestDto pageRequest() {
        return PageRequestDto.builder()
                .page(0)
                .size(20)
                .sortBy("createdAt")
                .sortDirection(SortDirection.ASC)
                .build();
    }

    private EmployeeLookupItemDto activeEmployee() {
        return new EmployeeLookupItemDto(EMPLOYEE_ID, "Ada Lovelace", true, POSITION_ID, GRADE_ID);
    }

    private void deny(UUID employeeId) {
        doThrow(new AccessDeniedException("Нет доступа к данным выбранного сотрудника"))
                .when(accessService).requireSelfOrManager(employeeId);
    }

    private void authenticate() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId(USER_ID)
                .role(SecurityConstants.EMPLOYEE_AUTHORITY)
                .email("ada@company.com")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        List.of(new SimpleGrantedAuthority(SecurityConstants.EMPLOYEE_AUTHORITY))
                )
        );
    }
}
