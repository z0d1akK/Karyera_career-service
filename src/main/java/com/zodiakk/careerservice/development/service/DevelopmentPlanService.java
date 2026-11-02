package com.zodiakk.careerservice.development.service;

import com.zodiakk.careerservice.career.repository.EmployeeCareerGoalRepository;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityStatus;
import com.zodiakk.careerservice.common.enums.DevelopmentPlanStatus;
import com.zodiakk.careerservice.common.enums.EvidenceVerificationStatus;
import com.zodiakk.careerservice.common.enums.PlanReviewDecision;
import com.zodiakk.careerservice.common.exception.domain.DevelopmentPlanNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.EmptyDevelopmentPlanException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
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
import com.zodiakk.careerservice.development.repository.specification.developmentPlan.DevelopmentPlanPageableFactory;
import com.zodiakk.careerservice.development.repository.specification.developmentPlan.DevelopmentPlanSpecification;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.integration.project.dto.MemberEligibilityDto;
import com.zodiakk.careerservice.integration.project.dto.TaskCompletionEvidenceDto;
import com.zodiakk.careerservice.integration.project.service.ProjectDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DevelopmentPlanService {

    private static final String TASK_EVIDENCE_TYPE = "PROJECT_TASK";
    private static final String TASK_EVIDENCE_ALIAS = "TASK";
    private static final String PROJECT_EVIDENCE_TYPE = "PROJECT";

    private final DevelopmentPlanRepository planRepository;

    private final DevelopmentActivityRepository activityRepository;

    private final ActivityEvidenceRepository evidenceRepository;

    private final PlanReviewRepository reviewRepository;

    private final EmployeeCareerGoalRepository careerGoalRepository;

    private final DevelopmentPlanMapper planMapper;

    private final DevelopmentActivityMapper activityMapper;

    private final ActivityEvidenceMapper evidenceMapper;

    private final PlanReviewMapper reviewMapper;

    private final EmployeeDirectoryService employeeDirectory;

    private final ProjectDirectoryService projectDirectory;

    private final OutboxEventService outboxEventService;

    private final CareerAccessService accessService;

    public Page<DevelopmentPlanResponseDto> listByEmployee(
            UUID employeeId,
            DevelopmentPlanFilterDto filter,
            PageRequestDto pageRequest
    ) {
        accessService.requireSelfOrManager(employeeId);

        DevelopmentPlanFilterDto effectiveFilter =
                filter == null ? new DevelopmentPlanFilterDto() : filter;
        effectiveFilter.setEmployeeId(employeeId);

        return planRepository.findAll(
                        DevelopmentPlanSpecification.filter(effectiveFilter),
                        DevelopmentPlanPageableFactory.create(pageRequest)
                )
                .map(this::toResponse);
    }

    @Transactional
    public DevelopmentPlanResponseDto create(DevelopmentPlanCreateRequestDto dto) {
        accessService.requireSelfOrManager(dto.getEmployeeId());

        EmployeeLookupItemDto employee = employeeDirectory.requireEmployee(dto.getEmployeeId());

        if (!employee.active()) {
            throw new InvalidStateTransitionException("Неактивному сотруднику нельзя создать план развития");
        }

        if (dto.getCareerGoalId() != null) {
            validateCareerGoal(dto.getCareerGoalId(), dto.getEmployeeId());
        }

        validateDates(dto.getStartDate(), dto.getTargetDate());

        DevelopmentPlan plan = DevelopmentPlan.builder()
                .employeeId(dto.getEmployeeId())
                .careerGoalId(dto.getCareerGoalId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .status(DevelopmentPlanStatus.DRAFT)
                .startDate(dto.getStartDate())
                .targetDate(dto.getTargetDate())
                .build();

        DevelopmentPlan saved = planRepository.save(plan);

        if (dto.getActivities() != null) {
            for (DevelopmentPlanActivityItemDto item : dto.getActivities()) {
                createActivity(
                        saved,
                        item.getTitle(),
                        item.getDescription(),
                        item.getActivityType(),
                        item.getSkillId(),
                        item.getDueDate()
                );
            }
        }

        return toResponse(saved);
    }

    public DevelopmentPlanResponseDto get(UUID planId) {
        DevelopmentPlan plan = findPlan(planId);
        accessService.requireSelfOrManager(plan.getEmployeeId());

        return toResponse(plan);
    }

    @Transactional
    public DevelopmentPlanResponseDto update(UUID planId, DevelopmentPlanUpdateRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireSelfOrManager(plan.getEmployeeId());

        assertEditable(plan);

        if (dto.getCareerGoalId() != null) {
            validateCareerGoal(dto.getCareerGoalId(), plan.getEmployeeId());
        }

        if (dto.getStartDate() != null || dto.getTargetDate() != null) {
            validateDates(
                    dto.getStartDate() != null ? dto.getStartDate() : plan.getStartDate(),
                    dto.getTargetDate() != null ? dto.getTargetDate() : plan.getTargetDate()
            );
        }

        DevelopmentPlanStatus previousStatus = plan.getStatus();

        planMapper.updateEntity(dto, plan);
        plan.setStatus(previousStatus);

        return toResponse(planRepository.saveAndFlush(plan));
    }

    @Transactional
    public DevelopmentPlanResponseDto submit(UUID planId, DevelopmentPlanSubmitRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireSelfOrManager(plan.getEmployeeId());

        checkVersion(plan.getVersion(), dto.getExpectedVersion());

        if (plan.getStatus() != DevelopmentPlanStatus.DRAFT
                && plan.getStatus() != DevelopmentPlanStatus.CHANGES_REQUESTED) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    DevelopmentPlanStatus.SUBMITTED.name()
            );
        }

        plan.setStatus(DevelopmentPlanStatus.SUBMITTED);

        return toResponse(planRepository.saveAndFlush(plan));
    }

    @Transactional
    public DevelopmentPlanResponseDto approve(UUID planId, DevelopmentPlanReviewRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireManagerOrHr(plan.getEmployeeId());

        checkVersion(plan.getVersion(), dto.getExpectedVersion());

        if (plan.getStatus() != DevelopmentPlanStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    DevelopmentPlanStatus.ACTIVE.name()
            );
        }

        if (activityRepository.countByPlanId(planId) == 0) {
            throw new EmptyDevelopmentPlanException(planId);
        }

        OffsetDateTime now = OffsetDateTime.now();

        plan.setStatus(DevelopmentPlanStatus.ACTIVE);
        plan.setApprovedBy(accessService.currentEmployeeId());
        plan.setApprovedAt(now);

        saveReview(planId, PlanReviewDecision.APPROVED, dto.getComment(), now);

        DevelopmentPlan saved = planRepository.saveAndFlush(plan);

        outboxEventService.saveEvent(
                planId,
                KafkaEventConstants.AGGREGATE_TYPE_DEVELOPMENT_PLAN,
                KafkaEventConstants.DEVELOPMENT_PLAN_APPROVED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "planId", planId,
                        "employeeId", saved.getEmployeeId(),
                        "approvedAt", now
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public DevelopmentPlanResponseDto requestChanges(UUID planId, DevelopmentPlanReviewRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireManagerOrHr(plan.getEmployeeId());

        checkVersion(plan.getVersion(), dto.getExpectedVersion());

        if (plan.getStatus() != DevelopmentPlanStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    DevelopmentPlanStatus.CHANGES_REQUESTED.name()
            );
        }

        plan.setStatus(DevelopmentPlanStatus.CHANGES_REQUESTED);

        saveReview(planId, PlanReviewDecision.CHANGES_REQUESTED, dto.getComment(), OffsetDateTime.now());

        return toResponse(planRepository.saveAndFlush(plan));
    }

    @Transactional
    public DevelopmentPlanResponseDto complete(UUID planId, DevelopmentPlanCompletionRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireManagerOrHr(plan.getEmployeeId());

        checkVersion(plan.getVersion(), dto.getExpectedVersion());

        if (plan.getStatus() != DevelopmentPlanStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    DevelopmentPlanStatus.COMPLETED.name()
            );
        }

        List<DevelopmentActivity> activities = activityRepository.findAllByPlanIdOrderByDueDateAsc(planId);

        boolean hasPending = activities.stream()
                .filter(activity -> activity.getStatus() != DevelopmentActivityStatus.CANCELLED)
                .anyMatch(activity -> activity.getStatus() != DevelopmentActivityStatus.COMPLETED);

        if (hasPending) {
            throw new InvalidStateTransitionException(
                    "План развития нельзя завершить, пока не подтверждены все мероприятия"
            );
        }

        plan.setStatus(DevelopmentPlanStatus.COMPLETED);

        saveReview(planId, PlanReviewDecision.COMPLETION_CONFIRMED, dto.getComment(), OffsetDateTime.now());

        return toResponse(planRepository.saveAndFlush(plan));
    }

    @Transactional
    public DevelopmentPlanResponseDto cancel(UUID planId, DevelopmentPlanCompletionRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireSelfOrManager(plan.getEmployeeId());

        checkVersion(plan.getVersion(), dto.getExpectedVersion());

        if (plan.getStatus() == DevelopmentPlanStatus.COMPLETED
                || plan.getStatus() == DevelopmentPlanStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    DevelopmentPlanStatus.CANCELLED.name()
            );
        }

        plan.setStatus(DevelopmentPlanStatus.CANCELLED);

        return toResponse(planRepository.saveAndFlush(plan));
    }

    public List<PlanReviewResponseDto> reviews(UUID planId) {
        DevelopmentPlan plan = findPlan(planId);
        accessService.requireSelfOrManager(plan.getEmployeeId());

        return reviewRepository.findAllByPlanIdOrderByReviewedAtDesc(planId)
                .stream()
                .map(reviewMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public DevelopmentActivityResponseDto addActivity(UUID planId, DevelopmentActivityCreateRequestDto dto) {
        DevelopmentPlan plan = findPlanForUpdate(planId);
        accessService.requireSelfOrManager(plan.getEmployeeId());

        assertActivitiesEditable(plan);

        DevelopmentActivity activity = createActivity(
                plan,
                dto.getTitle(),
                dto.getDescription(),
                dto.getActivityType(),
                dto.getSkillId(),
                dto.getDueDate()
        );

        return toResponse(activity);
    }

    @Transactional
    public DevelopmentActivityResponseDto updateActivity(UUID activityId, DevelopmentActivityUpdateRequestDto dto) {
        DevelopmentActivity activity = findActivityForUpdate(activityId);
        DevelopmentPlan plan = findPlan(activity.getPlanId());
        accessService.requireSelfOrManager(plan.getEmployeeId());

        assertActivitiesEditable(plan);

        if (activity.getStatus() == DevelopmentActivityStatus.COMPLETED
                || activity.getStatus() == DevelopmentActivityStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "Мероприятие",
                    activity.getStatus().name(),
                    "изменение"
            );
        }

        if (dto.getSkillId() != null) {
            employeeDirectory.requireSkill(dto.getSkillId());
        }

        DevelopmentActivityStatus previousStatus = activity.getStatus();

        activityMapper.updateEntity(dto, activity);
        activity.setStatus(previousStatus);

        return toResponse(activityRepository.saveAndFlush(activity));
    }

    @Transactional
    public DevelopmentActivityResponseDto submitActivity(
            UUID activityId,
            DevelopmentActivitySubmitRequestDto dto
    ) {
        DevelopmentActivity activity = findActivityForUpdate(activityId);
        DevelopmentPlan plan = findPlan(activity.getPlanId());
        accessService.requireSelfOrManager(plan.getEmployeeId());

        checkVersion(activity.getVersion(), dto.getExpectedVersion());

        if (plan.getStatus() != DevelopmentPlanStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Заявить о выполнении мероприятия можно только в активном плане развития"
            );
        }

        if (activity.getStatus() != DevelopmentActivityStatus.PLANNED
                && activity.getStatus() != DevelopmentActivityStatus.IN_PROGRESS
                && activity.getStatus() != DevelopmentActivityStatus.REJECTED) {
            throw new InvalidStateTransitionException(
                    "Мероприятие",
                    activity.getStatus().name(),
                    DevelopmentActivityStatus.SUBMITTED.name()
            );
        }

        activity.setStatus(DevelopmentActivityStatus.SUBMITTED);

        return toResponse(activityRepository.saveAndFlush(activity));
    }

    @Transactional
    public DevelopmentActivityResponseDto verifyActivity(
            UUID activityId,
            DevelopmentActivityVerificationRequestDto dto
    ) {
        DevelopmentActivity activity = findActivityForUpdate(activityId);
        DevelopmentPlan plan = findPlan(activity.getPlanId());
        accessService.requireManagerOrHr(plan.getEmployeeId());

        checkVersion(activity.getVersion(), dto.getExpectedVersion());

        if (activity.getStatus() != DevelopmentActivityStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "Мероприятие",
                    activity.getStatus().name(),
                    DevelopmentActivityStatus.COMPLETED.name()
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        activity.setStatus(DevelopmentActivityStatus.COMPLETED);
        activity.setCompletedAt(now);

        verifyEvidence(activityId, EvidenceVerificationStatus.VERIFIED, now);

        DevelopmentActivity saved = activityRepository.saveAndFlush(activity);

        outboxEventService.saveEvent(
                activityId,
                KafkaEventConstants.AGGREGATE_TYPE_DEVELOPMENT_ACTIVITY,
                KafkaEventConstants.DEVELOPMENT_ACTIVITY_COMPLETED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "activityId", activityId,
                        "planId", plan.getId(),
                        "employeeId", plan.getEmployeeId(),
                        "skillId", activity.getSkillId() == null ? "" : activity.getSkillId().toString(),
                        "completedAt", now
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public DevelopmentActivityResponseDto rejectActivity(
            UUID activityId,
            DevelopmentActivityVerificationRequestDto dto
    ) {
        DevelopmentActivity activity = findActivityForUpdate(activityId);
        DevelopmentPlan plan = findPlan(activity.getPlanId());
        accessService.requireManagerOrHr(plan.getEmployeeId());

        checkVersion(activity.getVersion(), dto.getExpectedVersion());

        if (activity.getStatus() != DevelopmentActivityStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "Мероприятие",
                    activity.getStatus().name(),
                    DevelopmentActivityStatus.REJECTED.name()
            );
        }

        activity.setStatus(DevelopmentActivityStatus.REJECTED);

        verifyEvidence(activityId, EvidenceVerificationStatus.REJECTED, OffsetDateTime.now());

        return toResponse(activityRepository.saveAndFlush(activity));
    }

    @Transactional
    public ActivityEvidenceResponseDto addEvidence(UUID activityId, ActivityEvidenceCreateRequestDto dto) {
        DevelopmentActivity activity = findActivityForUpdate(activityId);
        DevelopmentPlan plan = findPlan(activity.getPlanId());
        accessService.requireSelfOrManager(plan.getEmployeeId());

        if (activity.getStatus() == DevelopmentActivityStatus.COMPLETED
                || activity.getStatus() == DevelopmentActivityStatus.CANCELLED) {
            throw new InvalidStateTransitionException(
                    "Нельзя добавить подтверждение к завершённому или отменённому мероприятию"
            );
        }

        validatePracticalEvidence(dto, plan.getEmployeeId());

        ActivityEvidence evidence = ActivityEvidence.builder()
                .activityId(activityId)
                .evidenceType(dto.getEvidenceType())
                .externalEntityId(dto.getExternalEntityId())
                .externalUrl(dto.getExternalUrl())
                .description(dto.getDescription())
                .verificationStatus(EvidenceVerificationStatus.PENDING)
                .build();

        return evidenceMapper.toResponseDto(evidenceRepository.save(evidence));
    }

    public DevelopmentProgressResponseDto progress(UUID employeeId) {
        accessService.requireSelfOrManager(employeeId);

        List<DevelopmentPlan> plans = planRepository.findAllByEmployeeId(employeeId);

        long activePlans = plans.stream()
                .filter(plan -> plan.getStatus() == DevelopmentPlanStatus.ACTIVE)
                .count();

        long totalActivities = 0;
        long completedActivities = 0;

        for (DevelopmentPlan plan : plans) {
            for (DevelopmentActivity activity : activityRepository.findAllByPlanIdOrderByDueDateAsc(plan.getId())) {
                if (activity.getStatus() == DevelopmentActivityStatus.CANCELLED) {
                    continue;
                }

                totalActivities++;

                if (activity.getStatus() == DevelopmentActivityStatus.COMPLETED) {
                    completedActivities++;
                }
            }
        }

        return DevelopmentProgressResponseDto.builder()
                .employeeId(employeeId)
                .totalPlans((long) plans.size())
                .activePlans(activePlans)
                .totalActivities(totalActivities)
                .completedActivities(completedActivities)
                .progressPercent(percent(completedActivities, totalActivities))
                .plans(plans.stream().map(this::toResponse).toList())
                .build();
    }

    private DevelopmentActivity createActivity(
            DevelopmentPlan plan,
            String title,
            String description,
            com.zodiakk.careerservice.common.enums.DevelopmentActivityType type,
            UUID skillId,
            java.time.LocalDate dueDate
    ) {
        if (skillId != null) {
            employeeDirectory.requireSkill(skillId);
        }

        DevelopmentActivity activity = DevelopmentActivity.builder()
                .planId(plan.getId())
                .skillId(skillId)
                .title(title)
                .description(description)
                .activityType(type)
                .status(DevelopmentActivityStatus.PLANNED)
                .dueDate(dueDate)
                .assignedBy(accessService.currentEmployeeId())
                .build();

        return activityRepository.save(activity);
    }

    private void validatePracticalEvidence(ActivityEvidenceCreateRequestDto dto, UUID employeeId) {
        if (dto.getExternalEntityId() == null) {
            return;
        }

        String type = dto.getEvidenceType() == null ? "" : dto.getEvidenceType().toUpperCase();

        if (TASK_EVIDENCE_TYPE.equals(type) || TASK_EVIDENCE_ALIAS.equals(type)) {
            TaskCompletionEvidenceDto evidence = projectDirectory.getTaskCompletionEvidence(
                    dto.getExternalEntityId(),
                    employeeId
            );

            if (!evidence.taskCompleted() || !evidence.employeeParticipated()) {
                throw new InvalidStateTransitionException(
                        "Задача не завершена или сотрудник не связан с её выполнением"
                );
            }

            return;
        }

        if (PROJECT_EVIDENCE_TYPE.equals(type)) {
            MemberEligibilityDto eligibility = projectDirectory.getMemberEligibility(
                    dto.getExternalEntityId(),
                    employeeId
            );

            if (!eligibility.eligible()) {
                throw new InvalidStateTransitionException(
                        "Сотрудник не является действующим участником проекта"
                );
            }
        }
    }

    private void verifyEvidence(UUID activityId, EvidenceVerificationStatus status, OffsetDateTime now) {
        List<ActivityEvidence> evidence = evidenceRepository.findAllByActivityId(activityId);

        UUID verifier = accessService.currentEmployeeId();

        for (ActivityEvidence item : evidence) {
            item.setVerificationStatus(status);
            item.setVerifiedBy(verifier);
            item.setVerifiedAt(now);
        }

        evidenceRepository.saveAll(evidence);
    }

    private void saveReview(UUID planId, PlanReviewDecision decision, String comment, OffsetDateTime now) {
        PlanReview review = PlanReview.builder()
                .planId(planId)
                .reviewerEmployeeId(accessService.currentEmployeeId())
                .decision(decision)
                .comment(comment)
                .reviewedAt(now)
                .build();

        reviewRepository.save(review);
    }

    public DevelopmentPlanResponseDto toResponse(DevelopmentPlan plan) {
        DevelopmentPlanResponseDto response = planMapper.toResponseDto(plan);

        List<DevelopmentActivity> activities =
                activityRepository.findAllByPlanIdOrderByDueDateAsc(plan.getId());

        long total = activities.stream()
                .filter(activity -> activity.getStatus() != DevelopmentActivityStatus.CANCELLED)
                .count();

        long completed = activities.stream()
                .filter(activity -> activity.getStatus() == DevelopmentActivityStatus.COMPLETED)
                .count();

        response.setTotalActivities(total);
        response.setCompletedActivities(completed);
        response.setProgressPercent(percent(completed, total));

        return response;
    }

    private DevelopmentActivityResponseDto toResponse(DevelopmentActivity activity) {
        DevelopmentActivityResponseDto response = activityMapper.toResponseDto(activity);

        response.setEvidence(
                evidenceRepository.findAllByActivityId(activity.getId())
                        .stream()
                        .map(evidenceMapper::toResponseDto)
                        .toList()
        );

        return response;
    }

    private int percent(long completed, long total) {
        if (total <= 0) {
            return 0;
        }

        return (int) Math.min(100L, completed * 100L / total);
    }

    private void validateCareerGoal(UUID careerGoalId, UUID employeeId) {
        careerGoalRepository.findById(careerGoalId)
                .filter(goal -> goal.getEmployeeId().equals(employeeId))
                .orElseThrow(() -> new InvalidStateTransitionException(
                        "Карьерная цель не найдена или принадлежит другому сотруднику"
                ));
    }

    private void validateDates(java.time.LocalDate start, java.time.LocalDate target) {
        if (start != null && target != null && target.isBefore(start)) {
            throw new InvalidStateTransitionException(
                    "Целевая дата плана развития не может быть раньше даты начала"
            );
        }
    }

    private void assertEditable(DevelopmentPlan plan) {
        if (plan.getStatus() != DevelopmentPlanStatus.DRAFT
                && plan.getStatus() != DevelopmentPlanStatus.CHANGES_REQUESTED) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    "изменение"
            );
        }
    }

    private void assertActivitiesEditable(DevelopmentPlan plan) {
        if (plan.getStatus() == DevelopmentPlanStatus.COMPLETED
                || plan.getStatus() == DevelopmentPlanStatus.CANCELLED
                || plan.getStatus() == DevelopmentPlanStatus.SUBMITTED) {
            throw new InvalidStateTransitionException(
                    "План развития",
                    plan.getStatus().name(),
                    "изменение мероприятий"
            );
        }
    }

    private void checkVersion(Long actualVersion, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(actualVersion)) {
            throw new OptimisticLockingFailureException(
                    "Ожидаемая версия не совпадает с текущей"
            );
        }
    }

    private DevelopmentPlan findPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> DevelopmentPlanNotFoundException.plan(planId));
    }

    private DevelopmentPlan findPlanForUpdate(UUID planId) {
        return planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> DevelopmentPlanNotFoundException.plan(planId));
    }

    private DevelopmentActivity findActivityForUpdate(UUID activityId) {
        return activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> DevelopmentPlanNotFoundException.activity(activityId));
    }
}
