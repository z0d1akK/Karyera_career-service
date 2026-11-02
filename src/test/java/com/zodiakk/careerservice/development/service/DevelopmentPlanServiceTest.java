package com.zodiakk.careerservice.development.service;

import com.zodiakk.careerservice.career.entity.EmployeeCareerGoal;
import com.zodiakk.careerservice.career.repository.EmployeeCareerGoalRepository;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityStatus;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityType;
import com.zodiakk.careerservice.common.enums.DevelopmentPlanStatus;
import com.zodiakk.careerservice.common.enums.EvidenceVerificationStatus;
import com.zodiakk.careerservice.common.enums.PlanReviewDecision;
import com.zodiakk.careerservice.common.exception.domain.DevelopmentPlanNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.EmptyDevelopmentPlanException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.common.exception.domain.ReferenceDataException;
import com.zodiakk.careerservice.development.dto.request.action.ActivityEvidenceCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivityCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivitySubmitRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivityVerificationRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanActivityItemDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanCompletionRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanReviewRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanSubmitRequestDto;
import com.zodiakk.careerservice.development.dto.request.filter.DevelopmentPlanFilterDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentActivityUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentPlanUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.ActivityEvidenceResponseDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentActivityResponseDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentPlanResponseDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentProgressResponseDto;
import com.zodiakk.careerservice.development.dto.response.PlanReviewResponseDto;
import com.zodiakk.careerservice.development.entity.ActivityEvidence;
import com.zodiakk.careerservice.development.entity.DevelopmentActivity;
import com.zodiakk.careerservice.development.entity.DevelopmentPlan;
import com.zodiakk.careerservice.development.entity.PlanReview;
import com.zodiakk.careerservice.development.mapper.ActivityEvidenceMapper;
import com.zodiakk.careerservice.development.mapper.DevelopmentActivityMapper;
import com.zodiakk.careerservice.development.mapper.DevelopmentPlanMapper;
import com.zodiakk.careerservice.development.mapper.PlanReviewMapper;
import com.zodiakk.careerservice.development.repository.ActivityEvidenceRepository;
import com.zodiakk.careerservice.development.repository.DevelopmentActivityRepository;
import com.zodiakk.careerservice.development.repository.DevelopmentPlanRepository;
import com.zodiakk.careerservice.development.repository.PlanReviewRepository;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.integration.project.dto.MemberEligibilityDto;
import com.zodiakk.careerservice.integration.project.dto.TaskCompletionEvidenceDto;
import com.zodiakk.careerservice.integration.project.service.ProjectDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelopmentPlanServiceTest {

    private static final UUID EMPLOYEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID PLAN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID OTHER_PLAN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID ACTIVITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID GOAL_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final UUID SKILL_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID EXTERNAL_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final UUID REVIEW_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final Long VERSION = 3L;

    @Mock
    private DevelopmentPlanRepository planRepository;

    @Mock
    private DevelopmentActivityRepository activityRepository;

    @Mock
    private ActivityEvidenceRepository evidenceRepository;

    @Mock
    private PlanReviewRepository reviewRepository;

    @Mock
    private EmployeeCareerGoalRepository careerGoalRepository;

    @Mock
    private DevelopmentPlanMapper planMapper;

    @Mock
    private DevelopmentActivityMapper activityMapper;

    @Mock
    private ActivityEvidenceMapper evidenceMapper;

    @Mock
    private PlanReviewMapper reviewMapper;

    @Mock
    private EmployeeDirectoryService employeeDirectory;

    @Mock
    private ProjectDirectoryService projectDirectory;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private CareerAccessService accessService;

    @InjectMocks
    private DevelopmentPlanService service;

    @Nested
    @DisplayName("listByEmployee")
    class ListByEmployee {

        @Test
        @DisplayName("denies access before reading plans")
        void shouldDenyAccess() {
            doThrow(new AccessDeniedException("Нет доступа к данным выбранного сотрудника"))
                    .when(accessService).requireSelfOrManager(EMPLOYEE_ID);

            assertThatThrownBy(() -> service.listByEmployee(EMPLOYEE_ID, new DevelopmentPlanFilterDto(), page()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(planRepository);
        }

        @Test
        @DisplayName("scopes a null filter to the employee and maps the page")
        void shouldListWithDefaultFilter() {
            DevelopmentPlan plan = plan(DevelopmentPlanStatus.DRAFT);
            when(planRepository.findAll(anySpecification(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(plan)));
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            assertThatCode(() -> service.listByEmployee(EMPLOYEE_ID, null, page()))
                    .doesNotThrowAnyException();

            verify(accessService).requireSelfOrManager(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("forces the employee id onto the supplied filter")
        void shouldBindEmployeeFilter() {
            DevelopmentPlanFilterDto filter = new DevelopmentPlanFilterDto();
            DevelopmentActivity completed = activity(DevelopmentActivityStatus.COMPLETED);
            DevelopmentActivity cancelled = activity(DevelopmentActivityStatus.CANCELLED);
            DevelopmentActivity planned = activity(DevelopmentActivityStatus.PLANNED);

            when(planRepository.findAll(anySpecification(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(plan(DevelopmentPlanStatus.ACTIVE))));
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID))
                    .thenReturn(List.of(completed, cancelled, planned));

            var page = service.listByEmployee(EMPLOYEE_ID, filter, page());

            assertThat(filter.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(page.getContent()).singleElement().satisfies(response -> {
                assertThat(response.getTotalActivities()).isEqualTo(2L);
                assertThat(response.getCompletedActivities()).isEqualTo(1L);
                assertThat(response.getProgressPercent()).isEqualTo(50);
            });
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("denies access before checking the employee")
        void shouldDenyAccess() {
            doThrow(new AccessDeniedException("Нет доступа к данным выбранного сотрудника"))
                    .when(accessService).requireSelfOrManager(EMPLOYEE_ID);

            assertThatThrownBy(() -> service.create(createRequest(null)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(employeeDirectory, planRepository);
        }

        @Test
        @DisplayName("rejects an inactive employee")
        void shouldRejectInactiveEmployee() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(false));

            assertThatThrownBy(() -> service.create(createRequest(null)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Неактивному сотруднику нельзя создать план развития");

            verify(planRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a career goal that does not exist")
        void shouldRejectMissingCareerGoal() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(true));
            when(careerGoalRepository.findById(GOAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(createRequest(GOAL_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Карьерная цель не найдена или принадлежит другому сотруднику");
        }

        @Test
        @DisplayName("rejects a career goal that belongs to another employee")
        void shouldRejectForeignCareerGoal() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(true));
            when(careerGoalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal(OTHER_EMPLOYEE_ID)));

            assertThatThrownBy(() -> service.create(createRequest(GOAL_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Карьерная цель не найдена или принадлежит другому сотруднику");
        }

        @Test
        @DisplayName("rejects a target date that is earlier than the start date")
        void shouldRejectInvertedDates() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(true));
            DevelopmentPlanCreateRequestDto dto = createRequest(null);
            dto.setTargetDate(LocalDate.of(2025, 12, 31));

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Целевая дата плана развития не может быть раньше даты начала");
        }

        @Test
        @DisplayName("creates a draft plan and its activities for an active employee")
        void shouldCreateDraftWithActivities() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(true));
            when(careerGoalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal(EMPLOYEE_ID)));
            when(employeeDirectory.requireSkill(SKILL_ID))
                    .thenReturn(new ReferenceLookupItemDto(SKILL_ID, "Java", true));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(planRepository.save(any(DevelopmentPlan.class))).thenAnswer(invocation -> {
                DevelopmentPlan plan = invocation.getArgument(0);
                plan.setId(PLAN_ID);
                return plan;
            });
            when(activityRepository.save(any(DevelopmentActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            DevelopmentPlanCreateRequestDto dto = createRequest(GOAL_ID);
            dto.setActivities(List.of(
                    DevelopmentPlanActivityItemDto.builder()
                            .skillId(SKILL_ID)
                            .title("Course")
                            .description("Java")
                            .activityType(DevelopmentActivityType.TRAINING)
                            .dueDate(LocalDate.of(2026, 3, 1))
                            .build(),
                    DevelopmentPlanActivityItemDto.builder()
                            .title("Reading")
                            .activityType(DevelopmentActivityType.SELF_STUDY)
                            .build()
            ));

            DevelopmentPlanResponseDto response = service.create(dto);

            ArgumentCaptor<DevelopmentPlan> planCaptor = ArgumentCaptor.forClass(DevelopmentPlan.class);
            verify(planRepository).save(planCaptor.capture());
            assertThat(planCaptor.getValue().getStatus()).isEqualTo(DevelopmentPlanStatus.DRAFT);
            assertThat(planCaptor.getValue().getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(planCaptor.getValue().getCareerGoalId()).isEqualTo(GOAL_ID);
            assertThat(planCaptor.getValue().getStartDate()).isEqualTo(planCaptor.getValue().getTargetDate());

            ArgumentCaptor<DevelopmentActivity> activityCaptor = ArgumentCaptor.forClass(DevelopmentActivity.class);
            verify(activityRepository, org.mockito.Mockito.times(2)).save(activityCaptor.capture());
            assertThat(activityCaptor.getAllValues())
                    .allSatisfy(activity -> {
                        assertThat(activity.getPlanId()).isEqualTo(PLAN_ID);
                        assertThat(activity.getStatus()).isEqualTo(DevelopmentActivityStatus.PLANNED);
                        assertThat(activity.getAssignedBy()).isEqualTo(ACTOR_ID);
                    });
            assertThat(activityCaptor.getAllValues().get(0).getSkillId()).isEqualTo(SKILL_ID);
            assertThat(activityCaptor.getAllValues().get(1).getSkillId()).isNull();
            assertThat(response.getStatus()).isEqualTo(DevelopmentPlanStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("throws when the plan does not exist")
        void shouldThrowWhenMissing() {
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(PLAN_ID))
                    .isInstanceOf(DevelopmentPlanNotFoundException.class)
                    .hasMessage("План развития с идентификатором " + PLAN_ID + " не найден");

            verifyNoInteractions(accessService);
        }

        @Test
        @DisplayName("returns the plan after an access check")
        void shouldReturnPlan() {
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.DRAFT)));
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            assertThat(service.get(PLAN_ID).getId()).isEqualTo(PLAN_ID);

            verify(accessService).requireSelfOrManager(EMPLOYEE_ID);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"})
        @DisplayName("updates an editable plan and keeps the workflow status")
        void shouldUpdateEditablePlan(DevelopmentPlanStatus status) {
            DevelopmentPlan plan = plan(status);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(careerGoalRepository.findById(GOAL_ID)).thenReturn(Optional.of(goal(EMPLOYEE_ID)));
            doAnswer(invocation -> {
                DevelopmentPlan entity = invocation.getArgument(1);
                entity.setTitle("Updated");
                entity.setStatus(DevelopmentPlanStatus.CANCELLED);
                entity.setCareerGoalId(GOAL_ID);
                return null;
            }).when(planMapper).updateEntity(any(), eq(plan));
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            DevelopmentPlanResponseDto response = service.update(PLAN_ID, DevelopmentPlanUpdateRequestDto.builder()
                    .careerGoalId(GOAL_ID)
                    .title("Updated")
                    .build());

            assertThat(plan.getStatus()).isEqualTo(status);
            assertThat(plan.getTitle()).isEqualTo("Updated");
            assertThat(response.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("rejects changes once the plan has left the editable states")
        void shouldRejectNonEditablePlan() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));

            assertThatThrownBy(() -> service.update(PLAN_ID, DevelopmentPlanUpdateRequestDto.builder()
                    .title("Updated")
                    .build()))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии ACTIVE не может быть переведён в состояние изменение");

            verify(planMapper, never()).updateEntity(any(), any());
        }

        @Test
        @DisplayName("rejects a target date that is earlier than the effective start date")
        void shouldRejectInvertedDates() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.DRAFT)));

            assertThatThrownBy(() -> service.update(PLAN_ID, DevelopmentPlanUpdateRequestDto.builder()
                    .startDate(LocalDate.of(2026, 7, 1))
                    .build()))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Целевая дата плана развития не может быть раньше даты начала");
        }
    }

    @Nested
    @DisplayName("submit")
    class Submit {

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, names = {"DRAFT", "CHANGES_REQUESTED"})
        @DisplayName("submits a draft or a plan with requested changes")
        void shouldSubmitEditablePlan(DevelopmentPlanStatus status) {
            DevelopmentPlan plan = plan(status);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            DevelopmentPlanResponseDto response = service.submit(PLAN_ID, submitRequest(VERSION));

            assertThat(plan.getStatus()).isEqualTo(DevelopmentPlanStatus.SUBMITTED);
            assertThat(response.getStatus()).isEqualTo(DevelopmentPlanStatus.SUBMITTED);
        }

        @Test
        @DisplayName("skips the version check when the caller does not send one")
        void shouldAllowMissingExpectedVersion() {
            DevelopmentPlan plan = plan(DevelopmentPlanStatus.DRAFT);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            assertThatCode(() -> service.submit(PLAN_ID, submitRequest(null))).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(
                value = DevelopmentPlanStatus.class,
                names = {"SUBMITTED", "ACTIVE", "COMPLETED", "CANCELLED"}
        )
        @DisplayName("rejects submit from any other status")
        void shouldRejectSubmit(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.submit(PLAN_ID, submitRequest(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии %s не может быть переведён в состояние SUBMITTED"
                            .formatted(status.name()));
        }

        @Test
        @DisplayName("rejects a stale expected version")
        void shouldRejectVersionMismatch() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.DRAFT)));

            assertThatThrownBy(() -> service.submit(PLAN_ID, submitRequest(99L)))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessage("Ожидаемая версия не совпадает с текущей");

            verify(planRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("activates a submitted plan that has activities and publishes the approval")
        void shouldApproveSubmittedPlan() {
            DevelopmentPlan plan = plan(DevelopmentPlanStatus.SUBMITTED);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(activityRepository.countByPlanId(PLAN_ID)).thenReturn(1L);
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            DevelopmentPlanResponseDto response = service.approve(PLAN_ID, reviewRequest("ship it"));

            assertThat(plan.getStatus()).isEqualTo(DevelopmentPlanStatus.ACTIVE);
            assertThat(plan.getApprovedBy()).isEqualTo(ACTOR_ID);
            assertThat(plan.getApprovedAt()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(DevelopmentPlanStatus.ACTIVE);
            assertThat(response.getApprovedBy()).isEqualTo(ACTOR_ID);

            ArgumentCaptor<PlanReview> reviewCaptor = ArgumentCaptor.forClass(PlanReview.class);
            verify(reviewRepository).save(reviewCaptor.capture());
            assertThat(reviewCaptor.getValue().getDecision()).isEqualTo(PlanReviewDecision.APPROVED);
            assertThat(reviewCaptor.getValue().getReviewerEmployeeId()).isEqualTo(ACTOR_ID);
            assertThat(reviewCaptor.getValue().getComment()).isEqualTo("ship it");
            assertThat(reviewCaptor.getValue().getReviewedAt()).isEqualTo(plan.getApprovedAt());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
            verify(outboxEventService).saveEvent(
                    eq(PLAN_ID),
                    eq(KafkaEventConstants.AGGREGATE_TYPE_DEVELOPMENT_PLAN),
                    eq(KafkaEventConstants.DEVELOPMENT_PLAN_APPROVED),
                    eq(KafkaEventConstants.EVENT_VERSION_V1),
                    payload.capture()
            );
            assertThat(payload.getValue())
                    .containsEntry("planId", PLAN_ID)
                    .containsEntry("employeeId", EMPLOYEE_ID)
                    .containsEntry("approvedAt", plan.getApprovedAt());
        }

        @Test
        @DisplayName("rejects an empty submitted plan")
        void shouldRejectEmptyPlan() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.SUBMITTED)));
            when(activityRepository.countByPlanId(PLAN_ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.approve(PLAN_ID, reviewRequest("empty")))
                    .isInstanceOf(EmptyDevelopmentPlanException.class)
                    .hasMessage("План развития " + PLAN_ID + " не содержит мероприятий и не может быть утверждён");

            verify(planRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxEventService);
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "SUBMITTED")
        @DisplayName("rejects approval from any status other than submitted")
        void shouldRejectApproval(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.approve(PLAN_ID, reviewRequest("no")))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии %s не может быть переведён в состояние ACTIVE"
                            .formatted(status.name()));
        }

        @Test
        @DisplayName("propagates an access denial from the manager check")
        void shouldDenyAccess() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.SUBMITTED)));
            doThrow(new AccessDeniedException("Действие доступно руководителю сотрудника или HR"))
                    .when(accessService).requireManagerOrHr(EMPLOYEE_ID);

            assertThatThrownBy(() -> service.approve(PLAN_ID, reviewRequest("no")))
                    .isInstanceOf(AccessDeniedException.class);

            verify(planRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("requestChanges")
    class RequestChanges {

        @Test
        @DisplayName("returns a submitted plan to changes requested and stores the review")
        void shouldRequestChanges() {
            DevelopmentPlan plan = plan(DevelopmentPlanStatus.SUBMITTED);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            DevelopmentPlanResponseDto response = service.requestChanges(PLAN_ID, reviewRequest("revise"));

            assertThat(plan.getStatus()).isEqualTo(DevelopmentPlanStatus.CHANGES_REQUESTED);
            assertThat(response.getStatus()).isEqualTo(DevelopmentPlanStatus.CHANGES_REQUESTED);

            ArgumentCaptor<PlanReview> reviewCaptor = ArgumentCaptor.forClass(PlanReview.class);
            verify(reviewRepository).save(reviewCaptor.capture());
            assertThat(reviewCaptor.getValue().getDecision()).isEqualTo(PlanReviewDecision.CHANGES_REQUESTED);
            assertThat(reviewCaptor.getValue().getComment()).isEqualTo("revise");
            verifyNoInteractions(outboxEventService);
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "SUBMITTED")
        @DisplayName("rejects a change request from any status other than submitted")
        void shouldRejectRequest(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.requestChanges(PLAN_ID, reviewRequest("no")))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии %s не может быть переведён в состояние CHANGES_REQUESTED"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("completes an active plan when every remaining activity is completed")
        void shouldCompleteWhenActivitiesAreConfirmed() {
            DevelopmentPlan plan = plan(DevelopmentPlanStatus.ACTIVE);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of(
                    activity(DevelopmentActivityStatus.COMPLETED),
                    activity(DevelopmentActivityStatus.CANCELLED)
            ));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();

            DevelopmentPlanResponseDto response = service.complete(PLAN_ID, completionRequest("done"));

            assertThat(plan.getStatus()).isEqualTo(DevelopmentPlanStatus.COMPLETED);
            assertThat(response.getStatus()).isEqualTo(DevelopmentPlanStatus.COMPLETED);
            assertThat(response.getTotalActivities()).isEqualTo(1L);
            assertThat(response.getCompletedActivities()).isEqualTo(1L);
            assertThat(response.getProgressPercent()).isEqualTo(100);

            ArgumentCaptor<PlanReview> reviewCaptor = ArgumentCaptor.forClass(PlanReview.class);
            verify(reviewRepository).save(reviewCaptor.capture());
            assertThat(reviewCaptor.getValue().getDecision()).isEqualTo(PlanReviewDecision.COMPLETION_CONFIRMED);
        }

        @Test
        @DisplayName("completes an active plan that has no activities left to confirm")
        void shouldCompletePlanWithoutActivities() {
            DevelopmentPlan plan = plan(DevelopmentPlanStatus.ACTIVE);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();

            assertThat(service.complete(PLAN_ID, completionRequest(null)).getProgressPercent()).isZero();
        }

        @Test
        @DisplayName("rejects completion while a non-cancelled activity is still open")
        void shouldRejectPendingActivities() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of(
                    activity(DevelopmentActivityStatus.COMPLETED),
                    activity(DevelopmentActivityStatus.SUBMITTED)
            ));

            assertThatThrownBy(() -> service.complete(PLAN_ID, completionRequest("early")))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития нельзя завершить, пока не подтверждены все мероприятия");
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
        @DisplayName("rejects completion from any status other than active")
        void shouldRejectCompletion(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.complete(PLAN_ID, completionRequest("no")))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии %s не может быть переведён в состояние COMPLETED"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @ParameterizedTest
        @EnumSource(
                value = DevelopmentPlanStatus.class,
                names = {"DRAFT", "SUBMITTED", "ACTIVE", "CHANGES_REQUESTED"}
        )
        @DisplayName("cancels a plan that is not already finished")
        void shouldCancelOpenPlan(DevelopmentPlanStatus status) {
            DevelopmentPlan plan = plan(status);
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan));
            when(planRepository.saveAndFlush(plan)).thenReturn(plan);
            stubPlanMapper();
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of());

            assertThat(service.cancel(PLAN_ID, completionRequest("stop")).getStatus())
                    .isEqualTo(DevelopmentPlanStatus.CANCELLED);
            verify(reviewRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, names = {"COMPLETED", "CANCELLED"})
        @DisplayName("rejects cancellation of a completed or already cancelled plan")
        void shouldRejectFinishedPlan(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.cancel(PLAN_ID, completionRequest("no")))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии %s не может быть переведён в состояние CANCELLED"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("reviews")
    class Reviews {

        @Test
        @DisplayName("returns reviews newest first after an access check")
        void shouldListReviews() {
            PlanReview review = PlanReview.builder()
                    .id(REVIEW_ID)
                    .planId(PLAN_ID)
                    .decision(PlanReviewDecision.APPROVED)
                    .build();
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(reviewRepository.findAllByPlanIdOrderByReviewedAtDesc(PLAN_ID)).thenReturn(List.of(review));
            when(reviewMapper.toResponseDto(review))
                    .thenReturn(PlanReviewResponseDto.builder().id(REVIEW_ID).build());

            assertThat(service.reviews(PLAN_ID)).extracting(PlanReviewResponseDto::getId).containsExactly(REVIEW_ID);
            verify(accessService).requireSelfOrManager(EMPLOYEE_ID);
        }
    }

    @Nested
    @DisplayName("addActivity")
    class AddActivity {

        @ParameterizedTest
        @EnumSource(
                value = DevelopmentPlanStatus.class,
                names = {"DRAFT", "CHANGES_REQUESTED", "ACTIVE"}
        )
        @DisplayName("adds a planned activity while the plan still accepts changes")
        void shouldAddActivity(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(activityRepository.save(any(DevelopmentActivity.class))).thenAnswer(invocation -> {
                DevelopmentActivity activity = invocation.getArgument(0);
                activity.setId(ACTIVITY_ID);
                return activity;
            });
            stubActivityMapper();
            when(evidenceRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of());

            DevelopmentActivityResponseDto response = service.addActivity(PLAN_ID, activityRequest(null));

            assertThat(response.getStatus()).isEqualTo(DevelopmentActivityStatus.PLANNED);
            ArgumentCaptor<DevelopmentActivity> captor = ArgumentCaptor.forClass(DevelopmentActivity.class);
            verify(activityRepository).save(captor.capture());
            assertThat(captor.getValue().getAssignedBy()).isEqualTo(ACTOR_ID);
            assertThat(captor.getValue().getSkillId()).isNull();
            verify(employeeDirectory, never()).requireSkill(any());
        }

        @Test
        @DisplayName("checks the skill before saving an activity that targets one")
        void shouldRequireSkill() {
            when(planRepository.findByIdForUpdate(PLAN_ID))
                    .thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(employeeDirectory.requireSkill(SKILL_ID))
                    .thenThrow(ReferenceDataException.skillNotFound(SKILL_ID));

            assertThatThrownBy(() -> service.addActivity(PLAN_ID, activityRequest(SKILL_ID)))
                    .isInstanceOf(ReferenceDataException.class)
                    .hasMessage("Навык с идентификатором " + SKILL_ID + " не найден");

            verify(activityRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, names = {"SUBMITTED", "COMPLETED", "CANCELLED"})
        @DisplayName("rejects activity changes on a submitted, completed or cancelled plan")
        void shouldRejectLockedPlan(DevelopmentPlanStatus status) {
            when(planRepository.findByIdForUpdate(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.addActivity(PLAN_ID, activityRequest(null)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("План развития в состоянии %s не может быть переведён в состояние изменение мероприятий"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("updateActivity")
    class UpdateActivity {

        @Test
        @DisplayName("updates an open activity and keeps its workflow status")
        void shouldUpdateOpenActivity() {
            DevelopmentActivity activity = activity(DevelopmentActivityStatus.IN_PROGRESS);
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(employeeDirectory.requireSkill(SKILL_ID))
                    .thenReturn(new ReferenceLookupItemDto(SKILL_ID, "Java", true));
            doAnswer(invocation -> {
                DevelopmentActivity entity = invocation.getArgument(1);
                entity.setTitle("Updated");
                entity.setStatus(DevelopmentActivityStatus.CANCELLED);
                return null;
            }).when(activityMapper).updateEntity(any(), eq(activity));
            when(activityRepository.saveAndFlush(activity)).thenReturn(activity);
            stubActivityMapper();
            when(evidenceRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of());

            DevelopmentActivityResponseDto response = service.updateActivity(
                    ACTIVITY_ID,
                    DevelopmentActivityUpdateRequestDto.builder().title("Updated").skillId(SKILL_ID).build()
            );

            assertThat(activity.getStatus()).isEqualTo(DevelopmentActivityStatus.IN_PROGRESS);
            assertThat(activity.getTitle()).isEqualTo("Updated");
            assertThat(response.getStatus()).isEqualTo(DevelopmentActivityStatus.IN_PROGRESS);
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentActivityStatus.class, names = {"COMPLETED", "CANCELLED"})
        @DisplayName("rejects changes to a completed or cancelled activity")
        void shouldRejectFinishedActivity(DevelopmentActivityStatus status) {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity(status)));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));

            assertThatThrownBy(() -> service.updateActivity(
                    ACTIVITY_ID,
                    DevelopmentActivityUpdateRequestDto.builder().title("Updated").build()
            ))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Мероприятие в состоянии %s не может быть переведён в состояние изменение"
                            .formatted(status.name()));
        }

        @Test
        @DisplayName("throws when the activity does not exist")
        void shouldThrowWhenMissing() {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateActivity(
                    ACTIVITY_ID,
                    DevelopmentActivityUpdateRequestDto.builder().title("Updated").build()
            ))
                    .isInstanceOf(DevelopmentPlanNotFoundException.class)
                    .hasMessage("Мероприятие плана развития с идентификатором " + ACTIVITY_ID + " не найдено");

            verify(planRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("submitActivity")
    class SubmitActivity {

        @ParameterizedTest
        @EnumSource(value = DevelopmentActivityStatus.class, names = {"PLANNED", "IN_PROGRESS", "REJECTED"})
        @DisplayName("submits an open activity of an active plan")
        void shouldSubmitAllowedStatus(DevelopmentActivityStatus status) {
            DevelopmentActivity activity = activity(status);
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(activityRepository.saveAndFlush(activity)).thenReturn(activity);
            stubActivityMapper();
            when(evidenceRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of());

            assertThat(service.submitActivity(ACTIVITY_ID, activitySubmit(VERSION)).getStatus())
                    .isEqualTo(DevelopmentActivityStatus.SUBMITTED);
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentActivityStatus.class, names = {"SUBMITTED", "COMPLETED", "CANCELLED"})
        @DisplayName("rejects submit from a status that is not open")
        void shouldRejectSubmit(DevelopmentActivityStatus status) {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity(status)));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));

            assertThatThrownBy(() -> service.submitActivity(ACTIVITY_ID, activitySubmit(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Мероприятие в состоянии %s не может быть переведён в состояние SUBMITTED"
                            .formatted(status.name()));
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentPlanStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
        @DisplayName("rejects submit unless the plan is active")
        void shouldRejectInactivePlan(DevelopmentPlanStatus status) {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID))
                    .thenReturn(Optional.of(activity(DevelopmentActivityStatus.PLANNED)));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(status)));

            assertThatThrownBy(() -> service.submitActivity(ACTIVITY_ID, activitySubmit(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Заявить о выполнении мероприятия можно только в активном плане развития");
        }
    }

    @Nested
    @DisplayName("verifyActivity")
    class VerifyActivity {

        @Test
        @DisplayName("completes a submitted activity, verifies evidence and publishes the event")
        void shouldVerifySubmittedActivity() {
            DevelopmentActivity activity = activity(DevelopmentActivityStatus.SUBMITTED);
            activity.setSkillId(SKILL_ID);
            ActivityEvidence evidence = evidence();
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(evidenceRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of(evidence));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(activityRepository.saveAndFlush(activity)).thenReturn(activity);
            stubActivityMapper();
            when(evidenceMapper.toResponseDto(evidence)).thenAnswer(invocation -> {
                ActivityEvidence item = invocation.getArgument(0);
                return ActivityEvidenceResponseDto.builder()
                        .verificationStatus(item.getVerificationStatus())
                        .verifiedBy(item.getVerifiedBy())
                        .build();
            });

            DevelopmentActivityResponseDto response = service.verifyActivity(ACTIVITY_ID, verification(VERSION));

            assertThat(activity.getStatus()).isEqualTo(DevelopmentActivityStatus.COMPLETED);
            assertThat(activity.getCompletedAt()).isNotNull();
            assertThat(evidence.getVerificationStatus()).isEqualTo(EvidenceVerificationStatus.VERIFIED);
            assertThat(evidence.getVerifiedBy()).isEqualTo(ACTOR_ID);
            assertThat(evidence.getVerifiedAt()).isEqualTo(activity.getCompletedAt());
            assertThat(response.getEvidence()).singleElement()
                    .extracting(ActivityEvidenceResponseDto::getVerificationStatus)
                    .isEqualTo(EvidenceVerificationStatus.VERIFIED);
            verify(evidenceRepository).saveAll(List.of(evidence));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
            verify(outboxEventService).saveEvent(
                    eq(ACTIVITY_ID),
                    eq(KafkaEventConstants.AGGREGATE_TYPE_DEVELOPMENT_ACTIVITY),
                    eq(KafkaEventConstants.DEVELOPMENT_ACTIVITY_COMPLETED),
                    eq(KafkaEventConstants.EVENT_VERSION_V1),
                    payload.capture()
            );
            assertThat(payload.getValue())
                    .containsEntry("activityId", ACTIVITY_ID)
                    .containsEntry("planId", PLAN_ID)
                    .containsEntry("employeeId", EMPLOYEE_ID)
                    .containsEntry("skillId", SKILL_ID.toString())
                    .containsEntry("completedAt", activity.getCompletedAt());
        }

        @Test
        @DisplayName("publishes an empty skill id when the activity has none")
        void shouldPublishEmptySkillId() {
            DevelopmentActivity activity = activity(DevelopmentActivityStatus.SUBMITTED);
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(evidenceRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of());
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(activityRepository.saveAndFlush(activity)).thenReturn(activity);
            stubActivityMapper();

            service.verifyActivity(ACTIVITY_ID, verification(VERSION));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
            verify(outboxEventService).saveEvent(any(), any(), any(), anyInt(), payload.capture());
            assertThat(payload.getValue()).containsEntry("skillId", "");
            verify(evidenceRepository).saveAll(List.of());
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentActivityStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "SUBMITTED")
        @DisplayName("rejects verification unless the activity was submitted")
        void shouldRejectVerification(DevelopmentActivityStatus status) {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity(status)));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));

            assertThatThrownBy(() -> service.verifyActivity(ACTIVITY_ID, verification(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Мероприятие в состоянии %s не может быть переведён в состояние COMPLETED"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("rejectActivity")
    class RejectActivity {

        @Test
        @DisplayName("rejects a submitted activity and its pending evidence without publishing an event")
        void shouldRejectSubmittedActivity() {
            DevelopmentActivity activity = activity(DevelopmentActivityStatus.SUBMITTED);
            ActivityEvidence evidence = evidence();
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
            when(evidenceRepository.findAllByActivityId(ACTIVITY_ID)).thenReturn(List.of(evidence));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(activityRepository.saveAndFlush(activity)).thenReturn(activity);
            stubActivityMapper();
            when(evidenceMapper.toResponseDto(any())).thenReturn(ActivityEvidenceResponseDto.builder().build());

            assertThat(service.rejectActivity(ACTIVITY_ID, verification(VERSION)).getStatus())
                    .isEqualTo(DevelopmentActivityStatus.REJECTED);
            assertThat(evidence.getVerificationStatus()).isEqualTo(EvidenceVerificationStatus.REJECTED);
            assertThat(evidence.getVerifiedBy()).isEqualTo(ACTOR_ID);
            assertThat(evidence.getVerifiedAt()).isNotNull();
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @ParameterizedTest
        @EnumSource(value = DevelopmentActivityStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "SUBMITTED")
        @DisplayName("rejects a decision unless the activity was submitted")
        void shouldRejectWhenNotSubmitted(DevelopmentActivityStatus status) {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity(status)));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));

            assertThatThrownBy(() -> service.rejectActivity(ACTIVITY_ID, verification(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Мероприятие в состоянии %s не может быть переведён в состояние REJECTED"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("addEvidence")
    class AddEvidence {

        @ParameterizedTest
        @EnumSource(value = DevelopmentActivityStatus.class, names = {"COMPLETED", "CANCELLED"})
        @DisplayName("rejects evidence on a finished activity")
        void shouldRejectFinishedActivity(DevelopmentActivityStatus status) {
            when(activityRepository.findByIdForUpdate(ACTIVITY_ID)).thenReturn(Optional.of(activity(status)));
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));

            assertThatThrownBy(() -> service.addEvidence(ACTIVITY_ID, evidenceRequest("PROJECT_TASK", EXTERNAL_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Нельзя добавить подтверждение к завершённому или отменённому мероприятию");

            verifyNoInteractions(projectDirectory);
        }

        @Test
        @DisplayName("stores pending evidence when no external entity is attached")
        void shouldStoreEvidenceWithoutExternalCheck() {
            arrangeOpenActivity();
            when(evidenceRepository.save(any(ActivityEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(evidenceMapper.toResponseDto(any(ActivityEvidence.class))).thenAnswer(invocation -> {
                ActivityEvidence item = invocation.getArgument(0);
                return ActivityEvidenceResponseDto.builder()
                        .verificationStatus(item.getVerificationStatus())
                        .evidenceType(item.getEvidenceType())
                        .build();
            });

            ActivityEvidenceResponseDto response = service.addEvidence(
                    ACTIVITY_ID,
                    evidenceRequest("PROJECT_TASK", null)
            );

            assertThat(response.getVerificationStatus()).isEqualTo(EvidenceVerificationStatus.PENDING);
            assertThat(response.getEvidenceType()).isEqualTo("PROJECT_TASK");
            verifyNoInteractions(projectDirectory);
        }

        @Test
        @DisplayName("accepts completed project-task evidence regardless of type case")
        void shouldAcceptProjectTaskEvidence() {
            arrangeOpenActivity();
            when(projectDirectory.getTaskCompletionEvidence(EXTERNAL_ID, EMPLOYEE_ID))
                    .thenReturn(taskEvidence(true, true));
            stubEvidenceSave();

            assertThat(service.addEvidence(ACTIVITY_ID, evidenceRequest("project_task", EXTERNAL_ID))
                    .getVerificationStatus()).isEqualTo(EvidenceVerificationStatus.PENDING);
        }

        @Test
        @DisplayName("accepts the TASK alias for project-task evidence")
        void shouldAcceptTaskAlias() {
            arrangeOpenActivity();
            when(projectDirectory.getTaskCompletionEvidence(EXTERNAL_ID, EMPLOYEE_ID))
                    .thenReturn(taskEvidence(true, true));
            stubEvidenceSave();

            service.addEvidence(ACTIVITY_ID, evidenceRequest("TASK", EXTERNAL_ID));

            verify(projectDirectory).getTaskCompletionEvidence(EXTERNAL_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("rejects task evidence when the task is not completed")
        void shouldRejectIncompleteTask() {
            arrangeOpenActivity();
            when(projectDirectory.getTaskCompletionEvidence(EXTERNAL_ID, EMPLOYEE_ID))
                    .thenReturn(taskEvidence(false, true));

            assertThatThrownBy(() -> service.addEvidence(ACTIVITY_ID, evidenceRequest("PROJECT_TASK", EXTERNAL_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Задача не завершена или сотрудник не связан с её выполнением");
        }

        @Test
        @DisplayName("rejects task evidence when the employee did not participate")
        void shouldRejectTaskWithoutParticipation() {
            arrangeOpenActivity();
            when(projectDirectory.getTaskCompletionEvidence(EXTERNAL_ID, EMPLOYEE_ID))
                    .thenReturn(taskEvidence(true, false));

            assertThatThrownBy(() -> service.addEvidence(ACTIVITY_ID, evidenceRequest("PROJECT_TASK", EXTERNAL_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Задача не завершена или сотрудник не связан с её выполнением");
        }

        @Test
        @DisplayName("accepts project evidence for an eligible member")
        void shouldAcceptProjectEvidence() {
            arrangeOpenActivity();
            when(projectDirectory.getMemberEligibility(EXTERNAL_ID, EMPLOYEE_ID))
                    .thenReturn(eligibility(true));
            stubEvidenceSave();

            service.addEvidence(ACTIVITY_ID, evidenceRequest("project", EXTERNAL_ID));

            verify(projectDirectory).getMemberEligibility(EXTERNAL_ID, EMPLOYEE_ID);
            verify(projectDirectory, never()).getTaskCompletionEvidence(any(), any());
        }

        @Test
        @DisplayName("rejects project evidence when the employee is not an active member")
        void shouldRejectIneligibleProjectMember() {
            arrangeOpenActivity();
            when(projectDirectory.getMemberEligibility(EXTERNAL_ID, EMPLOYEE_ID))
                    .thenReturn(eligibility(false));

            assertThatThrownBy(() -> service.addEvidence(ACTIVITY_ID, evidenceRequest("PROJECT", EXTERNAL_ID)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Сотрудник не является действующим участником проекта");
        }

        @Test
        @DisplayName("does not call the project service for a non-practical evidence type")
        void shouldIgnoreNonPracticalExternalEvidence() {
            arrangeOpenActivity();
            stubEvidenceSave();

            service.addEvidence(ACTIVITY_ID, evidenceRequest("LINK", EXTERNAL_ID));
            service.addEvidence(ACTIVITY_ID, evidenceRequest(null, EXTERNAL_ID));

            verifyNoInteractions(projectDirectory);
        }
    }

    @Nested
    @DisplayName("progress")
    class Progress {

        @Test
        @DisplayName("aggregates plans and skips cancelled activities")
        void shouldAggregateProgress() {
            DevelopmentPlan active = plan(PLAN_ID, DevelopmentPlanStatus.ACTIVE);
            DevelopmentPlan cancelledPlan = plan(OTHER_PLAN_ID, DevelopmentPlanStatus.CANCELLED);
            when(planRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(active, cancelledPlan));
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(PLAN_ID)).thenReturn(List.of(
                    activity(DevelopmentActivityStatus.COMPLETED),
                    activity(DevelopmentActivityStatus.CANCELLED),
                    activity(DevelopmentActivityStatus.PLANNED)
            ));
            when(activityRepository.findAllByPlanIdOrderByDueDateAsc(OTHER_PLAN_ID)).thenReturn(List.of(
                    activity(DevelopmentActivityStatus.COMPLETED)
            ));
            stubPlanMapper();

            DevelopmentProgressResponseDto progress = service.progress(EMPLOYEE_ID);

            assertThat(progress.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(progress.getTotalPlans()).isEqualTo(2L);
            assertThat(progress.getActivePlans()).isEqualTo(1L);
            assertThat(progress.getTotalActivities()).isEqualTo(3L);
            assertThat(progress.getCompletedActivities()).isEqualTo(2L);
            assertThat(progress.getProgressPercent()).isEqualTo(66);
            assertThat(progress.getPlans()).extracting(DevelopmentPlanResponseDto::getProgressPercent)
                    .containsExactly(50, 100);
        }

        @Test
        @DisplayName("reports zero progress when the employee has no plans")
        void shouldReportEmptyProgress() {
            when(planRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());

            DevelopmentProgressResponseDto progress = service.progress(EMPLOYEE_ID);

            assertThat(progress.getTotalPlans()).isZero();
            assertThat(progress.getActivePlans()).isZero();
            assertThat(progress.getTotalActivities()).isZero();
            assertThat(progress.getCompletedActivities()).isZero();
            assertThat(progress.getProgressPercent()).isZero();
            assertThat(progress.getPlans()).isEmpty();
            verifyNoInteractions(activityRepository);
        }
    }

    private void arrangeOpenActivity() {
        when(activityRepository.findByIdForUpdate(ACTIVITY_ID))
                .thenReturn(Optional.of(activity(DevelopmentActivityStatus.PLANNED)));
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan(DevelopmentPlanStatus.ACTIVE)));
    }

    private void stubEvidenceSave() {
        when(evidenceRepository.save(any(ActivityEvidence.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenceMapper.toResponseDto(any(ActivityEvidence.class))).thenAnswer(invocation -> {
            ActivityEvidence item = invocation.getArgument(0);
            return ActivityEvidenceResponseDto.builder()
                    .verificationStatus(item.getVerificationStatus())
                    .build();
        });
    }

    private void stubPlanMapper() {
        when(planMapper.toResponseDto(any(DevelopmentPlan.class))).thenAnswer(invocation -> {
            DevelopmentPlan plan = invocation.getArgument(0);
            return DevelopmentPlanResponseDto.builder()
                    .id(plan.getId())
                    .employeeId(plan.getEmployeeId())
                    .status(plan.getStatus())
                    .title(plan.getTitle())
                    .approvedBy(plan.getApprovedBy())
                    .build();
        });
    }

    private void stubActivityMapper() {
        when(activityMapper.toResponseDto(any(DevelopmentActivity.class))).thenAnswer(invocation -> {
            DevelopmentActivity activity = invocation.getArgument(0);
            return DevelopmentActivityResponseDto.builder()
                    .id(activity.getId())
                    .status(activity.getStatus())
                    .skillId(activity.getSkillId())
                    .build();
        });
    }

    @SuppressWarnings("unchecked")
    private Specification<DevelopmentPlan> anySpecification() {
        return any(Specification.class);
    }

    private DevelopmentPlan plan(DevelopmentPlanStatus status) {
        return plan(PLAN_ID, status);
    }

    private DevelopmentPlan plan(UUID id, DevelopmentPlanStatus status) {
        return DevelopmentPlan.builder()
                .id(id)
                .employeeId(EMPLOYEE_ID)
                .title("Growth")
                .status(status)
                .version(VERSION)
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    private DevelopmentActivity activity(DevelopmentActivityStatus status) {
        return DevelopmentActivity.builder()
                .id(ACTIVITY_ID)
                .planId(PLAN_ID)
                .title("Shadowing")
                .activityType(DevelopmentActivityType.MENTORING)
                .status(status)
                .version(VERSION)
                .build();
    }

    private ActivityEvidence evidence() {
        return ActivityEvidence.builder()
                .id(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                .activityId(ACTIVITY_ID)
                .evidenceType("LINK")
                .verificationStatus(EvidenceVerificationStatus.PENDING)
                .build();
    }

    private EmployeeLookupItemDto employee(boolean active) {
        return new EmployeeLookupItemDto(EMPLOYEE_ID, "Ada Lovelace", active, null, null);
    }

    private EmployeeCareerGoal goal(UUID employeeId) {
        return EmployeeCareerGoal.builder()
                .id(GOAL_ID)
                .employeeId(employeeId)
                .build();
    }

    private DevelopmentPlanCreateRequestDto createRequest(UUID careerGoalId) {
        return DevelopmentPlanCreateRequestDto.builder()
                .employeeId(EMPLOYEE_ID)
                .careerGoalId(careerGoalId)
                .title("Growth")
                .description("Details")
                .startDate(LocalDate.of(2026, 1, 1))
                .targetDate(LocalDate.of(2026, 1, 1))
                .build();
    }

    private DevelopmentPlanSubmitRequestDto submitRequest(Long version) {
        return DevelopmentPlanSubmitRequestDto.builder().expectedVersion(version).comment("ready").build();
    }

    private DevelopmentPlanReviewRequestDto reviewRequest(String comment) {
        return DevelopmentPlanReviewRequestDto.builder()
                .decision(PlanReviewDecision.APPROVED)
                .comment(comment)
                .expectedVersion(VERSION)
                .build();
    }

    private DevelopmentPlanCompletionRequestDto completionRequest(String comment) {
        return DevelopmentPlanCompletionRequestDto.builder()
                .comment(comment)
                .expectedVersion(VERSION)
                .build();
    }

    private DevelopmentActivityCreateRequestDto activityRequest(UUID skillId) {
        return DevelopmentActivityCreateRequestDto.builder()
                .skillId(skillId)
                .title("Course")
                .activityType(DevelopmentActivityType.TRAINING)
                .build();
    }

    private DevelopmentActivitySubmitRequestDto activitySubmit(Long version) {
        return DevelopmentActivitySubmitRequestDto.builder().expectedVersion(version).comment("done").build();
    }

    private DevelopmentActivityVerificationRequestDto verification(Long version) {
        return DevelopmentActivityVerificationRequestDto.builder()
                .expectedVersion(version)
                .comment("checked")
                .build();
    }

    private ActivityEvidenceCreateRequestDto evidenceRequest(String type, UUID externalId) {
        return ActivityEvidenceCreateRequestDto.builder()
                .evidenceType(type)
                .externalEntityId(externalId)
                .description("proof")
                .build();
    }

    private TaskCompletionEvidenceDto taskEvidence(boolean completed, boolean participated) {
        return new TaskCompletionEvidenceDto(
                EXTERNAL_ID,
                EMPLOYEE_ID,
                completed,
                participated,
                OffsetDateTime.now()
        );
    }

    private MemberEligibilityDto eligibility(boolean eligible) {
        return new MemberEligibilityDto(
                EXTERNAL_ID,
                EMPLOYEE_ID,
                eligible,
                true,
                eligible,
                null,
                null,
                null,
                eligible ? null : "inactive"
        );
    }

    private PageRequestDto page() {
        return PageRequestDto.builder()
                .page(0)
                .size(20)
                .sortBy("createdAt")
                .sortDirection(SortDirection.ASC)
                .build();
    }
}
