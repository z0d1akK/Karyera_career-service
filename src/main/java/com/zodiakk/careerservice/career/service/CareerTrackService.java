package com.zodiakk.careerservice.career.service;

import com.zodiakk.careerservice.career.dto.request.action.CareerTrackNodeCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTrackPublicationRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTransitionCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.create.CareerTrackCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackFilterDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackNodeUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.AvailableCareerTrackDto;
import com.zodiakk.careerservice.career.dto.response.AvailableTargetDto;
import com.zodiakk.careerservice.career.dto.response.AvailableTargetsResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackNodeResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTransitionResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTrack;
import com.zodiakk.careerservice.career.entity.CareerTrackNode;
import com.zodiakk.careerservice.career.entity.CareerTrackRevision;
import com.zodiakk.careerservice.career.entity.CareerTransition;
import com.zodiakk.careerservice.career.mapper.CareerTrackMapper;
import com.zodiakk.careerservice.career.mapper.CareerTrackNodeMapper;
import com.zodiakk.careerservice.career.mapper.CareerTrackRevisionMapper;
import com.zodiakk.careerservice.career.mapper.CareerTransitionMapper;
import com.zodiakk.careerservice.career.repository.CareerTrackNodeRepository;
import com.zodiakk.careerservice.career.repository.CareerTrackRepository;
import com.zodiakk.careerservice.career.repository.CareerTrackRevisionRepository;
import com.zodiakk.careerservice.career.repository.CareerTransitionRepository;
import com.zodiakk.careerservice.career.repository.specification.careerTrack.CareerTrackPageableFactory;
import com.zodiakk.careerservice.career.repository.specification.careerTrack.CareerTrackSpecification;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.enums.CareerRevisionStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackStatus;
import com.zodiakk.careerservice.common.exception.domain.CareerTrackNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.CareerTrackStructureNotFoundException;
import com.zodiakk.careerservice.common.exception.domain.CyclicCareerTrackException;
import com.zodiakk.careerservice.common.exception.domain.DuplicateResourceException;
import com.zodiakk.careerservice.common.exception.domain.ImmutableResourceException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
import com.zodiakk.careerservice.security.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerTrackService {

    private final CareerTrackRepository trackRepository;

    private final CareerTrackNodeRepository nodeRepository;

    private final CareerTransitionRepository transitionRepository;

    private final CareerTrackRevisionRepository revisionRepository;

    private final CareerTrackMapper trackMapper;

    private final CareerTrackNodeMapper nodeMapper;

    private final CareerTransitionMapper transitionMapper;

    private final CareerTrackRevisionMapper revisionMapper;

    private final EmployeeDirectoryService employeeDirectory;

    private final OutboxEventService outboxEventService;

    private final CareerAccessService accessService;

    private final ObjectMapper objectMapper;

    public Page<CareerTrackResponseDto> list(CareerTrackFilterDto filter, PageRequestDto pageRequest) {
        CareerTrackFilterDto effectiveFilter = filter == null ? new CareerTrackFilterDto() : filter;

        if (!accessService.isHrOrAdmin()) {
            effectiveFilter.setStatus(CareerTrackStatus.PUBLISHED);
        }

        return trackRepository.findAll(
                        CareerTrackSpecification.filter(effectiveFilter),
                        CareerTrackPageableFactory.create(pageRequest)
                )
                .map(this::toResponse);
    }

    @Transactional
    public CareerTrackResponseDto create(CareerTrackCreateRequestDto dto) {
        accessService.requireHrOrAdmin();

        if (trackRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw DuplicateResourceException.careerTrackCode(dto.getCode());
        }

        CareerTrack track = trackMapper.toEntity(dto);
        track.setCode(dto.getCode().trim());
        track.setStatus(CareerTrackStatus.DRAFT);
        track.setCreatedBy(SecurityUtils.getCurrentUserId());

        return toResponse(trackRepository.save(track));
    }

    public CareerTrackResponseDto get(UUID trackId) {
        CareerTrack track = findTrack(trackId);

        if (!accessService.isHrOrAdmin() && track.getStatus() != CareerTrackStatus.PUBLISHED) {
            throw new CareerTrackNotFoundException(trackId);
        }

        return toResponse(track);
    }

    @Transactional
    public CareerTrackResponseDto update(UUID trackId, CareerTrackUpdateRequestDto dto) {
        accessService.requireHrOrAdmin();

        CareerTrack track = findTrackForUpdate(trackId);
        assertDraft(track);

        if (dto.getCode() != null
                && !dto.getCode().isBlank()
                && trackRepository.existsByCodeIgnoreCaseAndIdNot(dto.getCode(), trackId)) {
            throw DuplicateResourceException.careerTrackCode(dto.getCode());
        }

        trackMapper.updateEntity(dto, track);

        if (dto.getCode() != null) {
            track.setCode(dto.getCode().trim());
        }

        return toResponse(trackRepository.saveAndFlush(track));
    }

    @Transactional
    public CareerTrackNodeResponseDto addNode(UUID trackId, CareerTrackNodeCreateRequestDto dto) {
        accessService.requireHrOrAdmin();

        CareerTrack track = findTrackForUpdate(trackId);
        assertDraft(track);

        employeeDirectory.requirePosition(dto.getPositionId());
        employeeDirectory.requireGrade(dto.getGradeId());

        if (nodeRepository.existsByTrackIdAndPositionIdAndGradeId(
                trackId,
                dto.getPositionId(),
                dto.getGradeId()
        )) {
            throw DuplicateResourceException.trackNode();
        }

        CareerTrackNode node = CareerTrackNode.builder()
                .trackId(trackId)
                .positionId(dto.getPositionId())
                .gradeId(dto.getGradeId())
                .label(dto.getLabel())
                .description(dto.getDescription())
                .displayOrder(dto.getDisplayOrder())
                .isEntryPoint(Boolean.TRUE.equals(dto.getIsEntryPoint()))
                .build();

        return nodeMapper.toResponseDto(nodeRepository.save(node));
    }

    @Transactional
    public CareerTrackNodeResponseDto updateNode(UUID nodeId, CareerTrackNodeUpdateRequestDto dto) {
        accessService.requireHrOrAdmin();

        CareerTrackNode node = findNode(nodeId);
        CareerTrack track = findTrackForUpdate(node.getTrackId());
        assertDraft(track);

        UUID positionId = dto.getPositionId() != null ? dto.getPositionId() : node.getPositionId();
        UUID gradeId = dto.getGradeId() != null ? dto.getGradeId() : node.getGradeId();

        if (dto.getPositionId() != null) {
            employeeDirectory.requirePosition(positionId);
        }

        if (dto.getGradeId() != null) {
            employeeDirectory.requireGrade(gradeId);
        }

        if ((dto.getPositionId() != null || dto.getGradeId() != null)
                && nodeRepository.existsByTrackIdAndPositionIdAndGradeIdAndIdNot(
                node.getTrackId(),
                positionId,
                gradeId,
                nodeId
        )) {
            throw DuplicateResourceException.trackNode();
        }

        nodeMapper.updateEntity(dto, node);

        return nodeMapper.toResponseDto(nodeRepository.saveAndFlush(node));
    }

    @Transactional
    public void deleteNode(UUID nodeId) {
        accessService.requireHrOrAdmin();

        CareerTrackNode node = findNode(nodeId);
        CareerTrack track = findTrackForUpdate(node.getTrackId());
        assertDraft(track);

        List<CareerTransition> related = new ArrayList<>();
        related.addAll(transitionRepository.findAllBySourceNodeId(nodeId));
        related.addAll(transitionRepository.findAllByTargetNodeId(nodeId));

        transitionRepository.deleteAll(related);
        nodeRepository.delete(node);
    }

    @Transactional
    public CareerTransitionResponseDto addTransition(UUID trackId, CareerTransitionCreateRequestDto dto) {
        accessService.requireHrOrAdmin();

        CareerTrack track = findTrackForUpdate(trackId);
        assertDraft(track);

        CareerTrackNode source = findNode(dto.getSourceNodeId());
        CareerTrackNode target = findNode(dto.getTargetNodeId());

        if (!trackId.equals(source.getTrackId()) || !trackId.equals(target.getTrackId())) {
            throw new InvalidStateTransitionException(
                    "Оба узла перехода должны принадлежать карьерному треку " + trackId
            );
        }

        if (source.getId().equals(target.getId())) {
            throw new CyclicCareerTrackException();
        }

        if (transitionRepository.existsBySourceNodeIdAndTargetNodeId(source.getId(), target.getId())) {
            throw DuplicateResourceException.transition();
        }

        CareerTransition transition = CareerTransition.builder()
                .sourceNodeId(source.getId())
                .targetNodeId(target.getId())
                .transitionType(dto.getTransitionType())
                .description(dto.getDescription())
                .estimatedMonths(dto.getEstimatedMonths())
                .active(dto.getActive() == null || dto.getActive())
                .build();

        return transitionMapper.toResponseDto(transitionRepository.save(transition));
    }

    @Transactional
    public void deleteTransition(UUID transitionId) {
        accessService.requireHrOrAdmin();

        CareerTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> CareerTrackStructureNotFoundException.transition(transitionId));

        CareerTrackNode source = findNode(transition.getSourceNodeId());
        CareerTrack track = findTrackForUpdate(source.getTrackId());
        assertDraft(track);

        transitionRepository.delete(transition);
    }

    @Transactional
    public CareerTrackResponseDto publish(UUID trackId, CareerTrackPublicationRequestDto dto) {
        accessService.requireHrOrAdmin();

        CareerTrack track = findTrackForUpdate(trackId);

        if (track.getStatus() == CareerTrackStatus.ARCHIVED) {
            throw new InvalidStateTransitionException(
                    "Карьерный трек",
                    track.getStatus().name(),
                    CareerTrackStatus.PUBLISHED.name()
            );
        }

        validateVersion(dto == null ? null : dto.getExpectedVersion(), track.getVersion());

        List<CareerTrackNode> nodes = nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(trackId);

        if (nodes.isEmpty()) {
            throw new InvalidStateTransitionException(
                    "Пустой карьерный трек не может быть опубликован"
            );
        }

        List<CareerTransition> transitions = loadTrackTransitions(nodes);

        assertAcyclic(nodes, transitions);

        int nextRevision = revisionRepository.findTopByTrackIdOrderByRevisionDesc(trackId)
                .map(revision -> revision.getRevision() + 1)
                .orElse(1);

        revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                        trackId,
                        CareerRevisionStatus.PUBLISHED
                )
                .ifPresent(previous -> {
                    previous.setStatus(CareerRevisionStatus.ARCHIVED);
                    revisionRepository.saveAndFlush(previous);
                });

        OffsetDateTime now = OffsetDateTime.now();
        UUID publishedBy = SecurityUtils.getCurrentUserId();

        CareerTrackRevision revision = CareerTrackRevision.builder()
                .trackId(trackId)
                .revision(nextRevision)
                .status(CareerRevisionStatus.PUBLISHED)
                .snapshot(snapshot(track, nodes, transitions))
                .publishedAt(now)
                .publishedBy(publishedBy)
                .build();

        revisionRepository.save(revision);

        track.setStatus(CareerTrackStatus.PUBLISHED);
        trackRepository.saveAndFlush(track);

        outboxEventService.saveEvent(
                trackId,
                KafkaEventConstants.AGGREGATE_TYPE_CAREER_TRACK,
                KafkaEventConstants.CAREER_TRACK_PUBLISHED,
                KafkaEventConstants.EVENT_VERSION_V1,
                Map.of(
                        "trackId", trackId,
                        "code", track.getCode(),
                        "revision", nextRevision
                )
        );

        return toResponse(track);
    }

    @Transactional
    public CareerTrackResponseDto archive(UUID trackId, CareerTrackPublicationRequestDto dto) {
        accessService.requireHrOrAdmin();

        CareerTrack track = findTrackForUpdate(trackId);

        if (track.getStatus() != CareerTrackStatus.PUBLISHED) {
            throw new InvalidStateTransitionException(
                    "Карьерный трек",
                    track.getStatus().name(),
                    CareerTrackStatus.ARCHIVED.name()
            );
        }

        validateVersion(dto == null ? null : dto.getExpectedVersion(), track.getVersion());

        revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                        trackId,
                        CareerRevisionStatus.PUBLISHED
                )
                .ifPresent(revision -> {
                    revision.setStatus(CareerRevisionStatus.ARCHIVED);
                    revisionRepository.saveAndFlush(revision);
                });

        track.setStatus(CareerTrackStatus.ARCHIVED);

        return toResponse(trackRepository.saveAndFlush(track));
    }

    public List<AvailableCareerTrackDto> availableTracks(UUID employeeId) {
        accessService.requireSelfOrManager(employeeId);

        EmployeeLookupItemDto employee = employeeDirectory.requireEmployee(employeeId);

        if (employee.positionId() == null || employee.gradeId() == null) {
            return List.of();
        }

        List<AvailableCareerTrackDto> result = new ArrayList<>();

        for (CareerTrack track : trackRepository.findAllByStatus(CareerTrackStatus.PUBLISHED)) {
            nodeRepository.findByTrackIdAndPositionIdAndGradeId(
                            track.getId(),
                            employee.positionId(),
                            employee.gradeId()
                    )
                    .ifPresent(node -> result.add(AvailableCareerTrackDto.builder()
                            .trackId(track.getId())
                            .code(track.getCode())
                            .name(track.getName())
                            .type(track.getType())
                            .currentNodeId(node.getId())
                            .availableTargetNodeIds(
                                    transitionRepository.findAllBySourceNodeIdAndActiveTrue(node.getId())
                                            .stream()
                                            .map(CareerTransition::getTargetNodeId)
                                            .toList()
                            )
                            .build()));
        }

        return result;
    }

    public AvailableTargetsResponseDto availableTargets(UUID trackId, UUID employeeId) {
        CareerTrack track = findTrack(trackId);
        EmployeeLookupItemDto employee = employeeDirectory.requireEmployee(employeeId);

        if (track.getStatus() != CareerTrackStatus.PUBLISHED) {
            throw new CareerTrackNotFoundException(trackId);
        }

        AvailableTargetsResponseDto response = AvailableTargetsResponseDto.builder()
                .trackId(trackId)
                .employeeId(employeeId)
                .targets(List.of())
                .build();

        if (employee.positionId() == null || employee.gradeId() == null) {
            return response;
        }

        CareerTrackNode currentNode = nodeRepository.findByTrackIdAndPositionIdAndGradeId(
                        trackId,
                        employee.positionId(),
                        employee.gradeId()
                )
                .orElse(null);

        if (currentNode == null) {
            return response;
        }

        response.setCurrentNodeId(currentNode.getId());

        Map<UUID, CareerTrackNode> nodesById = new HashMap<>();
        nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(trackId)
                .forEach(node -> nodesById.put(node.getId(), node));

        List<AvailableTargetDto> targets = transitionRepository
                .findAllBySourceNodeIdAndActiveTrue(currentNode.getId())
                .stream()
                .map(transition -> {
                    CareerTrackNode target = nodesById.get(transition.getTargetNodeId());

                    return target == null
                            ? null
                            : AvailableTargetDto.builder()
                            .nodeId(target.getId())
                            .positionId(target.getPositionId())
                            .gradeId(target.getGradeId())
                            .label(target.getLabel())
                            .transitionType(transition.getTransitionType())
                            .estimatedMonths(transition.getEstimatedMonths())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        response.setTargets(targets);

        return response;
    }

    public CareerTrackResponseDto toResponse(CareerTrack track) {
        CareerTrackResponseDto response = trackMapper.toResponseDto(track);

        List<CareerTrackNode> nodes = nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(track.getId());
        List<CareerTransition> transitions = loadTrackTransitions(nodes);

        Map<UUID, List<CareerTransitionResponseDto>> transitionsBySource = new HashMap<>();

        for (CareerTransition transition : transitions) {
            transitionsBySource
                    .computeIfAbsent(transition.getSourceNodeId(), key -> new ArrayList<>())
                    .add(transitionMapper.toResponseDto(transition));
        }

        List<CareerTrackNodeResponseDto> nodeDtos = nodes.stream()
                .map(node -> {
                    CareerTrackNodeResponseDto nodeDto = nodeMapper.toResponseDto(node);
                    nodeDto.setOutgoingTransitions(
                            transitionsBySource.getOrDefault(node.getId(), List.of())
                    );
                    return nodeDto;
                })
                .toList();

        response.setNodes(nodeDtos);
        response.setTransitions(transitions.stream().map(transitionMapper::toResponseDto).toList());

        revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                        track.getId(),
                        CareerRevisionStatus.PUBLISHED
                )
                .ifPresent(revision -> response.setCurrentRevision(revisionMapper.toResponseDto(revision)));

        return response;
    }

    private List<CareerTransition> loadTrackTransitions(List<CareerTrackNode> nodes) {
        Set<UUID> nodeIds = new LinkedHashSet<>();
        nodes.forEach(node -> nodeIds.add(node.getId()));

        if (nodeIds.isEmpty()) {
            return List.of();
        }

        List<CareerTransition> transitions = new ArrayList<>(
                transitionRepository.findAllBySourceNodeIdIn(nodeIds)
        );

        Map<UUID, CareerTransition> unique = new LinkedHashMap<>();
        transitions.forEach(transition -> unique.put(transition.getId(), transition));

        return new ArrayList<>(unique.values());
    }

    private void assertAcyclic(List<CareerTrackNode> nodes, List<CareerTransition> transitions) {
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        nodes.forEach(node -> adjacency.put(node.getId(), new ArrayList<>()));
        transitions.forEach(transition -> adjacency
                .computeIfAbsent(transition.getSourceNodeId(), key -> new ArrayList<>())
                .add(transition.getTargetNodeId()));

        Map<UUID, Integer> state = new HashMap<>();

        for (CareerTrackNode node : nodes) {
            if (hasCycle(node.getId(), adjacency, state)) {
                throw new CyclicCareerTrackException();
            }
        }
    }

    private boolean hasCycle(UUID nodeId, Map<UUID, List<UUID>> adjacency, Map<UUID, Integer> state) {
        int current = state.getOrDefault(nodeId, 0);

        if (current == 1) {
            return true;
        }

        if (current == 2) {
            return false;
        }

        state.put(nodeId, 1);

        for (UUID target : adjacency.getOrDefault(nodeId, List.of())) {
            if (!adjacency.containsKey(target)) {
                continue;
            }

            if (hasCycle(target, adjacency, state)) {
                return true;
            }
        }

        state.put(nodeId, 2);

        return false;
    }

    private String snapshot(CareerTrack track, List<CareerTrackNode> nodes, List<CareerTransition> transitions) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", track.getCode());
        payload.put("name", track.getName());
        payload.put("type", track.getType());
        payload.put("nodes", nodes.stream().map(node -> {
            Map<String, Object> nodePayload = new LinkedHashMap<>();
            nodePayload.put("id", node.getId());
            nodePayload.put("positionId", node.getPositionId());
            nodePayload.put("gradeId", node.getGradeId());
            nodePayload.put("label", node.getLabel());
            nodePayload.put("displayOrder", node.getDisplayOrder());
            nodePayload.put("entryPoint", node.getIsEntryPoint());
            return nodePayload;
        }).toList());
        payload.put("transitions", transitions.stream().map(transition -> Map.of(
                "sourceNodeId", transition.getSourceNodeId(),
                "targetNodeId", transition.getTargetNodeId(),
                "transitionType", transition.getTransitionType(),
                "active", transition.getActive()
        )).toList());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Не удалось сформировать снимок карьерного трека", ex);
        }
    }

    private void validateVersion(Long expectedVersion, Long actualVersion) {
        if (expectedVersion != null && !expectedVersion.equals(actualVersion)) {
            throw new org.springframework.dao.OptimisticLockingFailureException(
                    "Ожидаемая версия карьерного трека не совпадает с текущей"
            );
        }
    }

    private void assertDraft(CareerTrack track) {
        if (track.getStatus() != CareerTrackStatus.DRAFT) {
            throw ImmutableResourceException.publishedTrackStructure();
        }
    }

    private CareerTrack findTrack(UUID trackId) {
        return trackRepository.findById(trackId)
                .orElseThrow(() -> new CareerTrackNotFoundException(trackId));
    }

    private CareerTrack findTrackForUpdate(UUID trackId) {
        return trackRepository.findByIdForUpdate(trackId)
                .orElseThrow(() -> new CareerTrackNotFoundException(trackId));
    }

    private CareerTrackNode findNode(UUID nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> CareerTrackStructureNotFoundException.node(nodeId));
    }
}
