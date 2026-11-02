package com.zodiakk.careerservice.talent.service;

import com.zodiakk.careerservice.career.service.PositionRequirementService;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.enums.ReserveCandidateStatus;
import com.zodiakk.careerservice.common.enums.ReserveDecisionType;
import com.zodiakk.careerservice.common.enums.TalentReserveStatus;
import com.zodiakk.careerservice.common.exception.domain.DuplicateResourceException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.common.exception.domain.TalentReserveNotFoundException;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartRequestDto;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartResponseDto;
import com.zodiakk.careerservice.integration.analytics.dto.TalentScreeningDto;
import com.zodiakk.careerservice.integration.analytics.service.AnalyticsIntegrationService;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
import com.zodiakk.careerservice.talent.dto.request.action.ReserveCandidateDecisionCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.action.ReserveCandidateNominationRequestDto;
import com.zodiakk.careerservice.talent.dto.request.action.TalentReserveTransitionRequestDto;
import com.zodiakk.careerservice.talent.dto.request.create.TalentReserveCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.filter.ReserveCandidateFilterDto;
import com.zodiakk.careerservice.talent.dto.request.filter.ReserveDecisionFilterDto;
import com.zodiakk.careerservice.talent.dto.request.filter.TalentReserveFilterDto;
import com.zodiakk.careerservice.talent.dto.request.update.TalentReserveUpdateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveCandidateResponseDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveDecisionResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveAnalysisDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveContextDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentScreeningResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentScreeningStartResponseDto;
import com.zodiakk.careerservice.talent.entity.ReserveCandidate;
import com.zodiakk.careerservice.talent.entity.ReserveDecision;
import com.zodiakk.careerservice.talent.entity.TalentReserve;
import com.zodiakk.careerservice.talent.mapper.ReserveCandidateMapper;
import com.zodiakk.careerservice.talent.mapper.ReserveDecisionMapper;
import com.zodiakk.careerservice.talent.mapper.TalentReserveMapper;
import com.zodiakk.careerservice.talent.repository.ReserveCandidateRepository;
import com.zodiakk.careerservice.talent.repository.ReserveDecisionRepository;
import com.zodiakk.careerservice.talent.repository.TalentReserveRepository;
import com.zodiakk.careerservice.talent.repository.specification.reserveCandidate.ReserveCandidatePageableFactory;
import com.zodiakk.careerservice.talent.repository.specification.reserveCandidate.ReserveCandidateSpecification;
import com.zodiakk.careerservice.talent.repository.specification.reserveDecision.ReserveDecisionPageableFactory;
import com.zodiakk.careerservice.talent.repository.specification.reserveDecision.ReserveDecisionSpecification;
import com.zodiakk.careerservice.talent.repository.specification.talentReserve.TalentReservePageableFactory;
import com.zodiakk.careerservice.talent.repository.specification.talentReserve.TalentReserveSpecification;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TalentReserveService {

    private static final List<ReserveCandidateStatus> ACTIVE_CANDIDATE_STATUSES = List.of(
            ReserveCandidateStatus.NOMINATED,
            ReserveCandidateStatus.UNDER_REVIEW,
            ReserveCandidateStatus.APPROVED
    );

    private final TalentReserveRepository reserveRepository;

    private final ReserveCandidateRepository candidateRepository;

    private final ReserveDecisionRepository decisionRepository;

    private final TalentReserveMapper reserveMapper;

    private final ReserveCandidateMapper candidateMapper;

    private final ReserveDecisionMapper decisionMapper;

    private final EmployeeDirectoryService employeeDirectory;

    private final PositionRequirementService requirementService;

    private final AnalyticsIntegrationService analyticsIntegrationService;

    private final OutboxEventService outboxEventService;

    private final CareerAccessService accessService;

    public Page<TalentReserveResponseDto> list(
            TalentReserveFilterDto filter,
            PageRequestDto pageRequest
    ) {
        accessService.requireManagerOrAbove();

        return reserveRepository.findAll(
                        TalentReserveSpecification.filter(filter),
                        TalentReservePageableFactory.create(pageRequest)
                )
                .map(this::toResponse);
    }

    @Transactional
    public TalentReserveResponseDto create(TalentReserveCreateRequestDto dto) {
        accessService.requireHrOrAdmin();

        employeeDirectory.requirePosition(dto.getTargetPositionId());
        employeeDirectory.requireGrade(dto.getTargetGradeId());

        validateEffectivePeriod(dto.getEffectiveFrom(), dto.getEffectiveTo());

        TalentReserve reserve = reserveMapper.toEntity(dto);
        reserve.setName(dto.getName().trim());
        reserve.setStatus(TalentReserveStatus.DRAFT);
        reserve.setCreatedBy(accessService.currentUserId());

        return toResponse(reserveRepository.saveAndFlush(reserve));
    }

    public TalentReserveResponseDto get(UUID reserveId) {
        accessService.requireManagerOrAbove();

        return toResponse(findReserve(reserveId));
    }

    @Transactional
    public TalentReserveResponseDto update(UUID reserveId, TalentReserveUpdateRequestDto dto) {
        accessService.requireHrOrAdmin();

        TalentReserve reserve = findReserveForUpdate(reserveId);

        if (reserve.getStatus() == TalentReserveStatus.CLOSED
                || reserve.getStatus() == TalentReserveStatus.ARCHIVED) {
            throw new InvalidStateTransitionException(
                    "Кадровый резерв",
                    reserve.getStatus().name(),
                    "изменение"
            );
        }

        if (dto.getTargetPositionId() != null) {
            employeeDirectory.requirePosition(dto.getTargetPositionId());
        }

        if (dto.getTargetGradeId() != null) {
            employeeDirectory.requireGrade(dto.getTargetGradeId());
        }

        TalentReserveStatus previousStatus = reserve.getStatus();

        reserveMapper.updateEntity(dto, reserve);
        reserve.setStatus(previousStatus);

        if (dto.getName() != null) {
            reserve.setName(dto.getName().trim());
        }

        validateEffectivePeriod(reserve.getEffectiveFrom(), reserve.getEffectiveTo());

        return toResponse(reserveRepository.saveAndFlush(reserve));
    }

    @Transactional
    public TalentReserveResponseDto activate(UUID reserveId, TalentReserveTransitionRequestDto dto) {
        accessService.requireHrOrAdmin();

        TalentReserve reserve = findReserveForUpdate(reserveId);

        checkVersion(reserve.getVersion(), dto == null ? null : dto.getExpectedVersion());

        if (reserve.getStatus() != TalentReserveStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Кадровый резерв",
                    reserve.getStatus().name(),
                    TalentReserveStatus.ACTIVE.name()
            );
        }

        reserve.setStatus(TalentReserveStatus.ACTIVE);

        return toResponse(reserveRepository.saveAndFlush(reserve));
    }

    @Transactional
    public TalentReserveResponseDto close(UUID reserveId, TalentReserveTransitionRequestDto dto) {
        accessService.requireHrOrAdmin();

        TalentReserve reserve = findReserveForUpdate(reserveId);

        checkVersion(reserve.getVersion(), dto == null ? null : dto.getExpectedVersion());

        if (reserve.getStatus() != TalentReserveStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Кадровый резерв",
                    reserve.getStatus().name(),
                    TalentReserveStatus.CLOSED.name()
            );
        }

        reserve.setStatus(TalentReserveStatus.CLOSED);

        return toResponse(reserveRepository.saveAndFlush(reserve));
    }

    public Page<ReserveCandidateResponseDto> candidates(
            UUID reserveId,
            ReserveCandidateFilterDto filter,
            PageRequestDto pageRequest
    ) {
        accessService.requireManagerOrAbove();

        findReserve(reserveId);

        ReserveCandidateFilterDto effectiveFilter =
                filter == null ? new ReserveCandidateFilterDto() : filter;
        effectiveFilter.setReserveId(reserveId);

        return candidateRepository.findAll(
                        ReserveCandidateSpecification.filter(effectiveFilter),
                        ReserveCandidatePageableFactory.create(pageRequest)
                )
                .map(this::toResponse);
    }

    @Transactional
    public ReserveCandidateResponseDto nominate(
            UUID reserveId,
            ReserveCandidateNominationRequestDto dto
    ) {
        accessService.requireHrOrAdmin();

        TalentReserve reserve = findReserveForUpdate(reserveId);

        if (reserve.getStatus() != TalentReserveStatus.DRAFT
                && reserve.getStatus() != TalentReserveStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Номинация в кадровый резерв",
                    reserve.getStatus().name(),
                    "номинация"
            );
        }

        EmployeeLookupItemDto employee = employeeDirectory.requireEmployee(dto.getEmployeeId());

        if (!employee.active()) {
            throw new InvalidStateTransitionException(
                    "Неактивного сотрудника нельзя включить в кадровый резерв"
            );
        }

        if (candidateRepository.existsByReserveIdAndEmployeeIdAndStatusIn(
                reserveId,
                dto.getEmployeeId(),
                ACTIVE_CANDIDATE_STATUSES
        )) {
            throw DuplicateResourceException.reserveCandidate();
        }

        ReserveCandidate candidate = ReserveCandidate.builder()
                .reserveId(reserveId)
                .employeeId(dto.getEmployeeId())
                .assessmentId(dto.getAssessmentId())
                .status(ReserveCandidateStatus.NOMINATED)
                .nominatedBy(accessService.currentEmployeeId())
                .nominatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .comment(dto.getComment())
                .build();

        return toResponse(candidateRepository.save(candidate));
    }

    public ReserveCandidateResponseDto getCandidate(UUID candidateId) {
        accessService.requireManagerOrAbove();

        return toResponse(findCandidate(candidateId));
    }

    @Transactional
    public ReserveDecisionResponseDto decide(
            UUID candidateId,
            ReserveCandidateDecisionCreateRequestDto dto
    ) {
        accessService.requireHrOrAdmin();

        ReserveCandidate snapshot = findCandidate(candidateId);
        TalentReserve reserve = findReserveForUpdate(snapshot.getReserveId());
        ReserveCandidate candidate = findCandidateForUpdate(candidateId);

        if (reserve.getStatus() == TalentReserveStatus.CLOSED
                || reserve.getStatus() == TalentReserveStatus.ARCHIVED) {
            throw new InvalidStateTransitionException(
                    "Кадровый резерв",
                    reserve.getStatus().name(),
                    "принятие решения по кандидату"
            );
        }

        checkVersion(candidate.getVersion(), dto.getExpectedVersion());

        if (!canApplyDecision(candidate.getStatus(), dto.getDecision())) {
            throw new InvalidStateTransitionException(
                    "Кандидат кадрового резерва",
                    candidate.getStatus().name(),
                    dto.getDecision().name()
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UUID decidedBy = accessService.currentEmployeeId();

        ReserveDecision decision = ReserveDecision.builder()
                .candidateId(candidateId)
                .decidedByEmployeeId(decidedBy)
                .decision(dto.getDecision())
                .comment(dto.getComment())
                .assessmentId(dto.getAssessmentId())
                .decidedAt(now)
                .build();

        ReserveDecision savedDecision = decisionRepository.save(decision);

        ReserveCandidateStatus status = statusFor(dto.getDecision());

        candidate.setStatus(status);

        if (dto.getAssessmentId() != null) {
            candidate.setAssessmentId(dto.getAssessmentId());
        }

        if (status == ReserveCandidateStatus.APPROVED && candidate.getIncludedAt() == null) {
            candidate.setIncludedAt(now);
        }

        if (status == ReserveCandidateStatus.REJECTED || status == ReserveCandidateStatus.REMOVED) {
            candidate.setRemovedAt(now);
        }

        if (status == ReserveCandidateStatus.APPROVED) {
            candidate.setRemovedAt(null);
        }

        candidateRepository.save(candidate);

        if (status == ReserveCandidateStatus.APPROVED) {
            outboxEventService.saveEvent(
                    candidateId,
                    KafkaEventConstants.AGGREGATE_TYPE_RESERVE_CANDIDATE,
                    KafkaEventConstants.TALENT_RESERVE_CANDIDATE_APPROVED,
                    KafkaEventConstants.EVENT_VERSION_V1,
                    Map.of(
                            "candidateId", candidateId,
                            "reserveId", candidate.getReserveId(),
                            "employeeId", candidate.getEmployeeId(),
                            "targetPositionId", reserve.getTargetPositionId(),
                            "targetGradeId", reserve.getTargetGradeId(),
                            "approvedAt", now
                    )
            );
        }

        return decisionMapper.toResponseDto(savedDecision);
    }

    public Page<ReserveDecisionResponseDto> decisions(
            UUID candidateId,
            PageRequestDto pageRequest
    ) {
        accessService.requireManagerOrAbove();

        findCandidate(candidateId);

        ReserveDecisionFilterDto filter = ReserveDecisionFilterDto.builder()
                .candidateId(candidateId)
                .build();

        return decisionRepository.findAll(
                        ReserveDecisionSpecification.filter(filter),
                        ReserveDecisionPageableFactory.create(pageRequest)
                )
                .map(decisionMapper::toResponseDto);
    }

    public TalentScreeningStartResponseDto startScreening(UUID reserveId) {
        accessService.requireHrOrAdmin();

        TalentReserve reserve = findReserve(reserveId);

        assertReserveActiveForScreening(reserve);

        UUID requirementId = requirementService
                .current(reserve.getTargetPositionId(), reserve.getTargetGradeId())
                .getId();

        ScreeningStartResponseDto response = analyticsIntegrationService.startScreening(
                new ScreeningStartRequestDto(
                        reserveId,
                        reserve.getTargetPositionId(),
                        reserve.getTargetGradeId(),
                        requirementId
                )
        );

        return TalentScreeningStartResponseDto.builder()
                .reserveId(reserveId)
                .jobId(response == null ? null : response.jobId())
                .status(response == null ? null : response.status())
                .build();
    }

    public TalentScreeningResponseDto getScreening(UUID reserveId, UUID jobId) {
        accessService.requireManagerOrAbove();

        findReserve(reserveId);

        TalentScreeningDto screening = analyticsIntegrationService.getScreening(jobId);

        return TalentScreeningResponseDto.builder()
                .reserveId(reserveId)
                .jobId(screening == null ? jobId : screening.jobId())
                .status(screening == null ? null : screening.status())
                .targetPositionId(screening == null ? null : screening.targetPositionId())
                .targetGradeId(screening == null ? null : screening.targetGradeId())
                .requirementId(screening == null ? null : screening.requirementId())
                .requirementRevision(screening == null ? null : screening.requirementRevision())
                .methodologyVersion(screening == null ? null : screening.methodologyVersion())
                .candidateCount(screening == null ? null : screening.candidateCount())
                .evaluatedCount(screening == null ? null : screening.evaluatedCount())
                .failedCount(screening == null ? null : screening.failedCount())
                .createdAt(screening == null ? null : screening.createdAt())
                .completedAt(screening == null ? null : screening.completedAt())
                .results(screening == null ? List.of() : screening.results())
                .build();
    }

    public TalentReserveAnalysisDto analysis(UUID reserveId) {
        accessService.requireManagerOrAbove();

        TalentReserve reserve = findReserve(reserveId);

        List<ReserveCandidate> candidates = candidateRepository.findAllByReserveId(reserveId);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReserveCandidateStatus status : ReserveCandidateStatus.values()) {
            byStatus.put(status.name(), 0L);
        }

        for (ReserveCandidate candidate : candidates) {
            byStatus.merge(candidate.getStatus().name(), 1L, Long::sum);
        }

        long activeCount = candidates.stream()
                .filter(candidate -> ACTIVE_CANDIDATE_STATUSES.contains(candidate.getStatus()))
                .count();

        long approvedCount = candidates.stream()
                .filter(candidate -> candidate.getStatus() == ReserveCandidateStatus.APPROVED)
                .count();

        Set<UUID> employeeIds = new LinkedHashSet<>();
        candidates.forEach(candidate -> employeeIds.add(candidate.getEmployeeId()));

        Map<UUID, EmployeeLookupItemDto> employees =
                employeeDirectory.findEmployees(employeeIds);

        Set<UUID> positionIds = new LinkedHashSet<>();
        Set<UUID> gradeIds = new LinkedHashSet<>();

        for (EmployeeLookupItemDto employee : employees.values()) {
            if (employee.positionId() != null) {
                positionIds.add(employee.positionId());
            }

            if (employee.gradeId() != null) {
                gradeIds.add(employee.gradeId());
            }
        }

        Map<UUID, ReferenceLookupItemDto> positions = employeeDirectory.findPositions(positionIds);
        Map<UUID, ReferenceLookupItemDto> grades = employeeDirectory.findGrades(gradeIds);

        Map<String, Long> byPosition = new LinkedHashMap<>();
        Map<String, Long> byGrade = new LinkedHashMap<>();

        for (ReserveCandidate candidate : candidates) {
            EmployeeLookupItemDto employee = employees.get(candidate.getEmployeeId());

            if (employee == null) {
                continue;
            }

            byPosition.merge(label(positions, employee.positionId(), "Не указана"), 1L, Long::sum);
            byGrade.merge(label(grades, employee.gradeId(), "Не указан"), 1L, Long::sum);
        }

        return TalentReserveAnalysisDto.builder()
                .reserveId(reserveId)
                .status(reserve.getStatus())
                .candidateCount((long) candidates.size())
                .activeCandidateCount(activeCount)
                .approvedCandidateCount(approvedCount)
                .byStatus(byStatus)
                .byPosition(byPosition)
                .byGrade(byGrade)
                .build();
    }

    public TalentReserveContextDto context(UUID reserveId) {
        TalentReserve reserve = findReserve(reserveId);

        List<ReserveCandidate> candidates = candidateRepository.findAllByReserveId(reserveId);

        long activeCount = candidates.stream()
                .filter(candidate -> ACTIVE_CANDIDATE_STATUSES.contains(candidate.getStatus()))
                .count();

        long approvedCount = candidates.stream()
                .filter(candidate -> candidate.getStatus() == ReserveCandidateStatus.APPROVED)
                .count();

        return TalentReserveContextDto.builder()
                .reserveId(reserveId)
                .name(reserve.getName())
                .status(reserve.getStatus())
                .targetPositionId(reserve.getTargetPositionId())
                .targetGradeId(reserve.getTargetGradeId())
                .effectiveFrom(reserve.getEffectiveFrom())
                .effectiveTo(reserve.getEffectiveTo())
                .candidateCount((long) candidates.size())
                .activeCandidateCount(activeCount)
                .approvedCandidateCount(approvedCount)
                .build();
    }

    private void assertReserveActiveForScreening(TalentReserve reserve) {
        if (reserve.getStatus() != TalentReserveStatus.ACTIVE) {
            throw new InvalidStateTransitionException(
                    "Кадровый резерв",
                    reserve.getStatus().name(),
                    "предварительный отбор"
            );
        }
    }

    private String label(
            Map<UUID, ReferenceLookupItemDto> references,
            UUID id,
            String fallback
    ) {
        if (id == null) {
            return fallback;
        }

        ReferenceLookupItemDto reference = references.get(id);

        return reference == null ? id.toString() : reference.name();
    }

    private void validateEffectivePeriod(java.time.LocalDate from, java.time.LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidStateTransitionException(
                    "Дата окончания действия кадрового резерва не может быть раньше даты начала"
            );
        }
    }

    private boolean canApplyDecision(
            ReserveCandidateStatus current,
            ReserveDecisionType decision
    ) {
        return switch (decision) {
            case APPROVE, REJECT, REQUEST_REVIEW ->
                    current == ReserveCandidateStatus.NOMINATED
                            || current == ReserveCandidateStatus.UNDER_REVIEW;
            case REMOVE ->
                    current == ReserveCandidateStatus.NOMINATED
                            || current == ReserveCandidateStatus.UNDER_REVIEW
                            || current == ReserveCandidateStatus.APPROVED;
        };
    }

    private ReserveCandidateStatus statusFor(ReserveDecisionType decision) {
        return switch (decision) {
            case APPROVE -> ReserveCandidateStatus.APPROVED;
            case REJECT -> ReserveCandidateStatus.REJECTED;
            case REQUEST_REVIEW -> ReserveCandidateStatus.UNDER_REVIEW;
            case REMOVE -> ReserveCandidateStatus.REMOVED;
        };
    }

    private void checkVersion(Long actualVersion, Long expectedVersion) {
        if (expectedVersion != null && !Objects.equals(expectedVersion, actualVersion)) {
            throw new OptimisticLockingFailureException(
                    "Ожидаемая версия не совпадает с текущей"
            );
        }
    }

    private TalentReserveResponseDto toResponse(TalentReserve reserve) {
        TalentReserveResponseDto response = reserveMapper.toResponseDto(reserve);
        response.setCandidateCount((long) candidateRepository.findAllByReserveId(reserve.getId()).size());
        return response;
    }

    private ReserveCandidateResponseDto toResponse(ReserveCandidate candidate) {
        ReserveCandidateResponseDto response = candidateMapper.toResponseDto(candidate);

        response.setDecisions(
                decisionRepository.findAllByCandidateIdOrderByDecidedAtDesc(candidate.getId())
                        .stream()
                        .map(decisionMapper::toResponseDto)
                        .toList()
        );

        return response;
    }

    private TalentReserve findReserve(UUID reserveId) {
        return reserveRepository.findById(reserveId)
                .orElseThrow(() -> TalentReserveNotFoundException.reserve(reserveId));
    }

    private TalentReserve findReserveForUpdate(UUID reserveId) {
        return reserveRepository.findByIdForUpdate(reserveId)
                .orElseThrow(() -> TalentReserveNotFoundException.reserve(reserveId));
    }

    private ReserveCandidate findCandidate(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> TalentReserveNotFoundException.candidate(candidateId));
    }

    private ReserveCandidate findCandidateForUpdate(UUID candidateId) {
        return candidateRepository.findByIdForUpdate(candidateId)
                .orElseThrow(() -> TalentReserveNotFoundException.candidate(candidateId));
    }
}
