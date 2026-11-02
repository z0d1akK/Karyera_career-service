package com.zodiakk.careerservice.career.service;

import com.zodiakk.careerservice.career.dto.request.action.PositionRequirementUpsertRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.RequiredSkillItemDto;
import com.zodiakk.careerservice.career.dto.request.filter.PositionRequirementFilterDto;
import com.zodiakk.careerservice.career.dto.response.PositionRequirementResponseDto;
import com.zodiakk.careerservice.career.dto.response.RequiredSkillResponseDto;
import com.zodiakk.careerservice.career.entity.PositionRequirement;
import com.zodiakk.careerservice.career.entity.RequiredSkill;
import com.zodiakk.careerservice.career.mapper.PositionRequirementMapper;
import com.zodiakk.careerservice.career.mapper.RequiredSkillMapper;
import com.zodiakk.careerservice.career.repository.PositionRequirementRepository;
import com.zodiakk.careerservice.career.repository.RequiredSkillRepository;
import com.zodiakk.careerservice.career.repository.specification.positionRequirement.PositionRequirementPageableFactory;
import com.zodiakk.careerservice.career.repository.specification.positionRequirement.PositionRequirementSpecification;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.enums.RequirementStatus;
import com.zodiakk.careerservice.common.exception.domain.DuplicateResourceException;
import com.zodiakk.careerservice.common.exception.domain.ImmutableResourceException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.common.exception.domain.PositionRequirementNotFoundException;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
import com.zodiakk.careerservice.security.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PositionRequirementService {

    private final PositionRequirementRepository requirementRepository;

    private final RequiredSkillRepository requiredSkillRepository;

    private final PositionRequirementMapper requirementMapper;

    private final RequiredSkillMapper requiredSkillMapper;

    private final EmployeeDirectoryService employeeDirectory;

    private final OutboxEventService outboxEventService;

    private final CareerAccessService accessService;

    public Page<PositionRequirementResponseDto> list(
            PositionRequirementFilterDto filter,
            PageRequestDto pageRequest
    ) {
        return requirementRepository.findAll(
                        PositionRequirementSpecification.filter(filter),
                        PositionRequirementPageableFactory.create(pageRequest)
                )
                .map(this::toResponse);
    }

    @Transactional
    public PositionRequirementResponseDto create(PositionRequirementUpsertRequestDto dto) {
        accessService.requireHrOrAdmin();

        employeeDirectory.requirePosition(dto.getPositionId());
        employeeDirectory.requireGrade(dto.getGradeId());

        int revision = requirementRepository
                .findTopByPositionIdAndGradeIdOrderByRevisionDesc(dto.getPositionId(), dto.getGradeId())
                .map(existing -> existing.getRevision() + 1)
                .orElse(1);

        PositionRequirement requirement = PositionRequirement.builder()
                .positionId(dto.getPositionId())
                .gradeId(dto.getGradeId())
                .revision(revision)
                .status(RequirementStatus.DRAFT)
                .description(dto.getDescription())
                .minExperienceMonths(dto.getMinExperienceMonths())
                .build();

        PositionRequirement saved = requirementRepository.save(requirement);
        replaceSkills(saved.getId(), dto.getSkills());

        return toResponse(saved);
    }

    public PositionRequirementResponseDto get(UUID requirementId) {
        return toResponse(findRequirement(requirementId));
    }

    @Transactional
    public PositionRequirementResponseDto update(UUID requirementId, PositionRequirementUpsertRequestDto dto) {
        accessService.requireHrOrAdmin();

        PositionRequirement requirement = findRequirementForUpdate(requirementId);

        if (requirement.getStatus() != RequirementStatus.DRAFT) {
            throw ImmutableResourceException.publishedRequirement();
        }

        if (!requirement.getPositionId().equals(dto.getPositionId())
                || !requirement.getGradeId().equals(dto.getGradeId())) {
            throw new InvalidStateTransitionException(
                    "Позиция и грейд версии требований не могут быть изменены"
            );
        }

        employeeDirectory.requirePosition(dto.getPositionId());
        employeeDirectory.requireGrade(dto.getGradeId());

        requirement.setDescription(dto.getDescription());
        requirement.setMinExperienceMonths(dto.getMinExperienceMonths());

        requirementRepository.saveAndFlush(requirement);
        replaceSkills(requirement.getId(), dto.getSkills());

        return toResponse(requirement);
    }

    @Transactional
    public PositionRequirementResponseDto publish(UUID requirementId) {
        accessService.requireHrOrAdmin();

        PositionRequirement requirement = findRequirementForUpdate(requirementId);

        if (requirement.getStatus() != RequirementStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Требования к должности",
                    requirement.getStatus().name(),
                    RequirementStatus.PUBLISHED.name()
            );
        }

        List<RequiredSkill> skills = requiredSkillRepository.findAllByRequirementId(requirementId);

        if (skills.isEmpty()) {
            throw new InvalidStateTransitionException(
                    "Требования к должности без навыков не могут быть опубликованы"
            );
        }

        requirementRepository.findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
                        requirement.getPositionId(),
                        requirement.getGradeId(),
                        RequirementStatus.PUBLISHED
                )
                .ifPresent(previous -> {
                    previous.setStatus(RequirementStatus.SUPERSEDED);
                    requirementRepository.saveAndFlush(previous);
                });

        requirement.setStatus(RequirementStatus.PUBLISHED);
        requirement.setPublishedAt(OffsetDateTime.now());
        requirement.setPublishedBy(SecurityUtils.getCurrentUserId());

        requirementRepository.saveAndFlush(requirement);

        outboxEventService.saveEvent(
                requirement.getId(),
                KafkaEventConstants.AGGREGATE_TYPE_POSITION_REQUIREMENT,
                KafkaEventConstants.POSITION_REQUIREMENT_PUBLISHED,
                KafkaEventConstants.EVENT_VERSION_V1,
                java.util.Map.of(
                        "requirementId", requirement.getId(),
                        "positionId", requirement.getPositionId(),
                        "gradeId", requirement.getGradeId(),
                        "revision", requirement.getRevision()
                )
        );

        return toResponse(requirement);
    }

    public PositionRequirementResponseDto current(UUID positionId, UUID gradeId) {
        PositionRequirement requirement = requirementRepository
                .findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
                        positionId,
                        gradeId,
                        RequirementStatus.PUBLISHED
                )
                .orElseThrow(() -> PositionRequirementNotFoundException.of(
                        "Актуальные требования для должности " + positionId
                                + " и грейда " + gradeId + " не найдены"
                ));

        return toResponse(requirement);
    }

    public List<PositionRequirementResponseDto> history(UUID positionId, UUID gradeId) {
        return requirementRepository
                .findAllByPositionIdAndGradeIdOrderByRevisionDesc(positionId, gradeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PositionRequirementResponseDto toResponse(PositionRequirement requirement) {
        PositionRequirementResponseDto response = requirementMapper.toResponseDto(requirement);

        response.setSkills(
                requiredSkillRepository.findAllByRequirementId(requirement.getId())
                        .stream()
                        .map(requiredSkillMapper::toResponseDto)
                        .toList()
        );

        return response;
    }

    private void replaceSkills(UUID requirementId, List<RequiredSkillItemDto> skills) {
        requiredSkillRepository.deleteAllByRequirementId(requirementId);

        if (skills == null || skills.isEmpty()) {
            return;
        }

        Set<UUID> uniqueSkills = new HashSet<>();

        for (RequiredSkillItemDto skill : skills) {
            if (!uniqueSkills.add(skill.getSkillId())) {
                throw DuplicateResourceException.requiredSkill();
            }

            employeeDirectory.requireSkill(skill.getSkillId());

            RequiredSkill entity = RequiredSkill.builder()
                    .requirementId(requirementId)
                    .skillId(skill.getSkillId())
                    .requiredLevel(skill.getRequiredLevel())
                    .weight(skill.getWeight())
                    .mandatory(skill.getMandatory())
                    .description(skill.getDescription())
                    .build();

            requiredSkillRepository.save(entity);
        }
    }

    private PositionRequirement findRequirement(UUID requirementId) {
        return requirementRepository.findById(requirementId)
                .orElseThrow(() -> PositionRequirementNotFoundException.requirement(requirementId));
    }

    private PositionRequirement findRequirementForUpdate(UUID requirementId) {
        return requirementRepository.findByIdForUpdate(requirementId)
                .orElseThrow(() -> PositionRequirementNotFoundException.requirement(requirementId));
    }
}
