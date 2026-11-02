package com.zodiakk.careerservice.career.service;

import com.zodiakk.careerservice.career.dto.request.action.CareerGoalCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerGoalTransitionRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.EmployeeCareerGoalFilterDto;
import com.zodiakk.careerservice.career.dto.request.update.EmployeeCareerGoalUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.EmployeeCareerGoalResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTrackNode;
import com.zodiakk.careerservice.career.entity.EmployeeCareerGoal;
import com.zodiakk.careerservice.career.mapper.EmployeeCareerGoalMapper;
import com.zodiakk.careerservice.career.repository.CareerTrackNodeRepository;
import com.zodiakk.careerservice.career.repository.CareerTrackRepository;
import com.zodiakk.careerservice.career.repository.EmployeeCareerGoalRepository;
import com.zodiakk.careerservice.career.repository.specification.employeeCareerGoal.EmployeeCareerGoalPageableFactory;
import com.zodiakk.careerservice.career.repository.specification.employeeCareerGoal.EmployeeCareerGoalSpecification;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.enums.CareerGoalStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackStatus;
import com.zodiakk.careerservice.common.exception.domain.ActiveCareerGoalExistsException;
import com.zodiakk.careerservice.common.exception.domain.CareerGoalNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.CareerTrackStructureNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
import com.zodiakk.careerservice.security.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerGoalService {

    private final EmployeeCareerGoalRepository goalRepository;

    private final CareerTrackNodeRepository nodeRepository;

    private final CareerTrackRepository trackRepository;

    private final EmployeeCareerGoalMapper goalMapper;

    private final EmployeeDirectoryService employeeDirectory;

    private final OutboxEventService outboxEventService;

    private final CareerAccessService accessService;

    public Page<EmployeeCareerGoalResponseDto> listByEmployee(
            UUID employeeId,
            EmployeeCareerGoalFilterDto filter,
            PageRequestDto pageRequest
    ) {
        accessService.requireSelfOrManager(employeeId);

        EmployeeCareerGoalFilterDto effectiveFilter =
                filter == null ? new EmployeeCareerGoalFilterDto() : filter;
        effectiveFilter.setEmployeeId(employeeId);

        Page<EmployeeCareerGoal> goals = goalRepository.findAll(
                EmployeeCareerGoalSpecification.filter(effectiveFilter),
                EmployeeCareerGoalPageableFactory.create(pageRequest)
        );

        Map<UUID, ReferenceLookupItemDto> positions = employeeDirectory.findPositions(
                goals.getContent().stream().map(EmployeeCareerGoal::getTargetPositionId).toList()
        );
        Map<UUID, ReferenceLookupItemDto> grades = employeeDirectory.findGrades(
                goals.getContent().stream().map(EmployeeCareerGoal::getTargetGradeId).toList()
        );

        return goals.map(goal -> toResponse(goal, positions, grades));
    }

    @Transactional
    public EmployeeCareerGoalResponseDto create(UUID employeeId, CareerGoalCreateRequestDto dto) {
        accessService.requireSelfOrManager(employeeId);

        EmployeeLookupItemDto employee = employeeDirectory.requireEmployee(employeeId);

        if (!employee.active()) {
            throw new InvalidStateTransitionException("Неактивному сотруднику нельзя создать карьерную цель");
        }

        employeeDirectory.requirePosition(dto.getTargetPositionId());
        employeeDirectory.requireGrade(dto.getTargetGradeId());

        validateTargetNode(dto.getTargetNodeId(), dto.getTargetPositionId(), dto.getTargetGradeId());

        EmployeeCareerGoal goal = EmployeeCareerGoal.builder()
                .employeeId(employeeId)
                .targetPositionId(dto.getTargetPositionId())
                .targetGradeId(dto.getTargetGradeId())
                .targetNodeId(dto.getTargetNodeId())
                .status(CareerGoalStatus.PROPOSED)
                .motivation(dto.getMotivation())
                .targetDate(dto.getTargetDate())
                .createdBy(SecurityUtils.getCurrentUserId())
                .build();

        EmployeeCareerGoal saved = goalRepository.save(goal);
        publishChanged(saved);

        return toResponse(saved);
    }

    public EmployeeCareerGoalResponseDto get(UUID goalId) {
        EmployeeCareerGoal goal = findGoal(goalId);
        accessService.requireSelfOrManager(goal.getEmployeeId());

        return toResponse(goal);
    }

    @Transactional
    public EmployeeCareerGoalResponseDto update(UUID goalId, EmployeeCareerGoalUpdateRequestDto dto) {
        EmployeeCareerGoal goal = findGoalForUpdate(goalId);
        accessService.requireSelfOrManager(goal.getEmployeeId());

        if (isTerminal(goal.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Карьерная цель",
                    goal.getStatus().name(),
                    "изменение"
            );
        }

        UUID targetPositionId = dto.getTargetPositionId() != null
                ? dto.getTargetPositionId()
                : goal.getTargetPositionId();
        UUID targetGradeId = dto.getTargetGradeId() != null
                ? dto.getTargetGradeId()
                : goal.getTargetGradeId();

        if (dto.getTargetPositionId() != null) {
            employeeDirectory.requirePosition(targetPositionId);
        }

        if (dto.getTargetGradeId() != null) {
            employeeDirectory.requireGrade(targetGradeId);
        }

        if (dto.getTargetPositionId() != null || dto.getTargetGradeId() != null) {
            validateTargetNode(dto.getTargetNodeId(), targetPositionId, targetGradeId);
        }

        CareerGoalStatus previousStatus = goal.getStatus();

        goalMapper.updateEntity(dto, goal);
        goal.setStatus(previousStatus);

        EmployeeCareerGoal saved = goalRepository.saveAndFlush(goal);
        publishChanged(saved);

        return toResponse(saved);
    }

    @Transactional
    public EmployeeCareerGoalResponseDto activate(UUID goalId, CareerGoalTransitionRequestDto dto) {
        return transition(goalId, dto, CareerGoalStatus.ACTIVE);
    }

    @Transactional
    public EmployeeCareerGoalResponseDto achieve(UUID goalId, CareerGoalTransitionRequestDto dto) {
        return transition(goalId, dto, CareerGoalStatus.ACHIEVED);
    }

    @Transactional
    public EmployeeCareerGoalResponseDto cancel(UUID goalId, CareerGoalTransitionRequestDto dto) {
        return transition(goalId, dto, CareerGoalStatus.CANCELLED);
    }

    private EmployeeCareerGoalResponseDto transition(
            UUID goalId,
            CareerGoalTransitionRequestDto dto,
            CareerGoalStatus target
    ) {
        EmployeeCareerGoal goal = findGoalForUpdate(goalId);
        accessService.requireSelfOrManager(goal.getEmployeeId());

        Long expectedVersion = dto == null ? null : dto.getExpectedVersion();

        if (expectedVersion != null && !expectedVersion.equals(goal.getVersion())) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Ожидаемая версия карьерной цели не совпадает с текущей"
            );
        }

        CareerGoalStatus current = goal.getStatus();

        boolean allowed = switch (target) {
            case ACTIVE -> current == CareerGoalStatus.PROPOSED;
            case ACHIEVED -> current == CareerGoalStatus.ACTIVE;
            case CANCELLED -> current == CareerGoalStatus.PROPOSED
                    || current == CareerGoalStatus.ACTIVE;
            default -> false;
        };

        if (!allowed) {
            throw new InvalidStateTransitionException(
                    "Карьерная цель",
                    current.name(),
                    target.name()
            );
        }

        if (target == CareerGoalStatus.ACTIVE) {
            ensureNoOtherActiveGoal(goal);
        }

        goal.setStatus(target);

        EmployeeCareerGoal saved = goalRepository.saveAndFlush(goal);
        publishChanged(saved);

        return toResponse(saved);
    }

    private void ensureNoOtherActiveGoal(EmployeeCareerGoal goal) {
        goalRepository.findFirstByEmployeeIdAndStatus(goal.getEmployeeId(), CareerGoalStatus.ACTIVE)
                .filter(active -> !active.getId().equals(goal.getId()))
                .ifPresent(active -> {
                    throw new ActiveCareerGoalExistsException(goal.getEmployeeId());
                });
    }

    private void validateTargetNode(UUID targetNodeId, UUID positionId, UUID gradeId) {
        if (targetNodeId == null) {
            return;
        }

        CareerTrackNode node = nodeRepository.findById(targetNodeId)
                .orElseThrow(() -> CareerTrackStructureNotFoundException.node(targetNodeId));

        if (!node.getPositionId().equals(positionId) || !node.getGradeId().equals(gradeId)) {
            throw new InvalidStateTransitionException(
                    "Целевой узел не соответствует выбранной должности и грейду"
            );
        }

        trackRepository.findById(node.getTrackId())
                .filter(track -> track.getStatus() == CareerTrackStatus.PUBLISHED)
                .orElseThrow(() -> new InvalidStateTransitionException(
                        "Целевой узел принадлежит неопубликованному карьерному треку"
                ));
    }

    private void publishChanged(EmployeeCareerGoal goal) {
        outboxEventService.saveEvent(
                goal.getId(),
                KafkaEventConstants.AGGREGATE_TYPE_CAREER_GOAL,
                KafkaEventConstants.CAREER_GOAL_CHANGED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "goalId", goal.getId(),
                        "employeeId", goal.getEmployeeId(),
                        "targetPositionId", goal.getTargetPositionId(),
                        "targetGradeId", goal.getTargetGradeId(),
                        "status", goal.getStatus().name()
                )
        );
    }

    private boolean isTerminal(CareerGoalStatus status) {
        return status == CareerGoalStatus.ACHIEVED || status == CareerGoalStatus.CANCELLED;
    }

    private EmployeeCareerGoalResponseDto toResponse(EmployeeCareerGoal goal) {
        return toResponse(
                goal,
                employeeDirectory.findPositions(List.of(goal.getTargetPositionId())),
                employeeDirectory.findGrades(List.of(goal.getTargetGradeId()))
        );
    }

    private EmployeeCareerGoalResponseDto toResponse(
            EmployeeCareerGoal goal,
            Map<UUID, ReferenceLookupItemDto> positions,
            Map<UUID, ReferenceLookupItemDto> grades
    ) {
        EmployeeCareerGoalResponseDto response = goalMapper.toResponseDto(goal);

        ReferenceLookupItemDto position = positions.get(goal.getTargetPositionId());
        ReferenceLookupItemDto grade = grades.get(goal.getTargetGradeId());

        response.setTargetPositionName(position == null ? null : position.name());
        response.setTargetGradeName(grade == null ? null : grade.name());

        return response;
    }

    private EmployeeCareerGoal findGoal(UUID goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new CareerGoalNotFoundException(goalId));
    }

    private EmployeeCareerGoal findGoalForUpdate(UUID goalId) {
        return goalRepository.findByIdForUpdate(goalId)
                .orElseThrow(() -> new CareerGoalNotFoundException(goalId));
    }
}
