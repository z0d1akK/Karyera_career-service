package com.zodiakk.careerservice.career.service;

import com.zodiakk.careerservice.career.dto.request.action.CareerTrackNodeCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTrackPublicationRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTransitionCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.create.CareerTrackCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackFilterDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackNodeUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.AvailableTargetsResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackNodeResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackRevisionResponseDto;
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
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.constants.SecurityConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import com.zodiakk.careerservice.common.enums.CareerRevisionStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackType;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerTrackServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID TRACK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID OTHER_TRACK_ID = UUID.fromString("23232323-2323-2323-2323-232323232323");

    private static final UUID NODE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID OTHER_NODE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID THIRD_NODE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID FOURTH_NODE_ID = UUID.fromString("56565656-5656-5656-5656-565656565656");

    private static final UUID FOREIGN_NODE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final UUID TRANSITION_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID OTHER_TRANSITION_ID = UUID.fromString("78787878-7878-7878-7878-787878787878");

    private static final UUID POSITION_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final UUID OTHER_POSITION_ID = UUID.fromString("89898989-8989-8989-8989-898989898989");

    private static final UUID GRADE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID OTHER_GRADE_ID = UUID.fromString("9a9a9a9a-9a9a-9a9a-9a9a-9a9a9a9a9a9a");

    private static final UUID EMPLOYEE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID REVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID PREVIOUS_REVISION_ID = UUID.fromString("bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbcbc");

    private static final Long VERSION = 4L;

    private static final String CODE = "ENG-BE";

    private static final String NAME = "Backend";

    @Mock
    private CareerTrackRepository trackRepository;

    @Mock
    private CareerTrackNodeRepository nodeRepository;

    @Mock
    private CareerTransitionRepository transitionRepository;

    @Mock
    private CareerTrackRevisionRepository revisionRepository;

    @Mock
    private CareerTrackMapper trackMapper;

    @Mock
    private CareerTrackNodeMapper nodeMapper;

    @Mock
    private CareerTransitionMapper transitionMapper;

    @Mock
    private CareerTrackRevisionMapper revisionMapper;

    @Mock
    private EmployeeDirectoryService employeeDirectory;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private CareerAccessService accessService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CareerTrackService careerTrackService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("list")
    class ListTracks {

        @Test
        @DisplayName("forces PUBLISHED for a non-HR user and keeps the other criteria")
        void shouldForcePublishedStatusForNonHr() {
            CareerTrackFilterDto filter = CareerTrackFilterDto.builder()
                    .code("ENG")
                    .name("Back")
                    .type(CareerTrackType.VERTICAL)
                    .status(CareerTrackStatus.DRAFT)
                    .build();
            CareerTrack track = track(CareerTrackStatus.PUBLISHED);
            when(trackRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(track), PageRequest.of(1, 5), 6));
            CareerTrackResponseDto response = stubEmptyGraph();

            Page<CareerTrackResponseDto> result = careerTrackService.list(filter, pageRequest());

            assertThat(filter.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
            assertThat(filter.getCode()).isEqualTo("ENG");
            assertThat(filter.getName()).isEqualTo("Back");
            assertThat(filter.getType()).isEqualTo(CareerTrackType.VERTICAL);
            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(response.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);

            Pageable pageable = assertListedWithStatus(CareerTrackStatus.PUBLISHED);
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("code").getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("keeps the requested status for HR")
        void shouldKeepRequestedStatusForHr() {
            CareerTrackFilterDto filter = CareerTrackFilterDto.builder()
                    .status(CareerTrackStatus.DRAFT)
                    .build();
            when(accessService.isHrOrAdmin()).thenReturn(true);
            when(trackRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<CareerTrackResponseDto> result = careerTrackService.list(filter, pageRequest());

            assertThat(result).isEmpty();
            assertThat(filter.getStatus()).isEqualTo(CareerTrackStatus.DRAFT);
            assertListedWithStatus(CareerTrackStatus.DRAFT);
            verifyNoInteractions(trackMapper);
        }

        @Test
        @DisplayName("forces PUBLISHED when a non-HR user omits the filter")
        void shouldForcePublishedStatusWhenFilterIsMissing() {
            when(trackRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(careerTrackService.list(null, pageRequest())).isEmpty();

            assertListedWithStatus(CareerTrackStatus.PUBLISHED);
        }

        @Test
        @DisplayName("does not add a status predicate when HR omits the filter")
        void shouldLeaveStatusUnsetWhenHrOmitsTheFilter() {
            when(accessService.isHrOrAdmin()).thenReturn(true);
            when(trackRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(careerTrackService.list(null, pageRequest())).isEmpty();

            assertListedWithoutFieldPredicates();
        }

        @Test
        @DisplayName("returns an empty page without calling the mapper")
        void shouldReturnEmptyPageWithoutMapping() {
            CareerTrackFilterDto filter = CareerTrackFilterDto.builder().code("missing").build();
            when(trackRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            Page<CareerTrackResponseDto> result = careerTrackService.list(filter, pageRequest());

            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(filter.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
            verifyNoInteractions(trackMapper, nodeMapper, transitionMapper);
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns a published track to a non-HR user")
        void shouldReturnPublishedTrackToNonHr() {
            CareerTrack track = track(CareerTrackStatus.PUBLISHED);
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track));
            CareerTrackResponseDto response = stubEmptyGraph();

            assertThat(careerTrackService.get(TRACK_ID)).isSameAs(response);
            assertThat(response.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
        }

        @ParameterizedTest
        @EnumSource(value = CareerTrackStatus.class, names = {"DRAFT", "ARCHIVED"})
        @DisplayName("hides an unpublished track from a non-HR user")
        void shouldHideUnpublishedTrackFromNonHr(CareerTrackStatus status) {
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track(status)));

            assertThatThrownBy(() -> careerTrackService.get(TRACK_ID))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verifyNoInteractions(trackMapper);
        }

        @ParameterizedTest
        @EnumSource(value = CareerTrackStatus.class, names = {"DRAFT", "ARCHIVED"})
        @DisplayName("returns an unpublished track to HR")
        void shouldReturnUnpublishedTrackToHr(CareerTrackStatus status) {
            when(accessService.isHrOrAdmin()).thenReturn(true);
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track(status)));
            CareerTrackResponseDto response = stubEmptyGraph();

            assertThat(careerTrackService.get(TRACK_ID)).isSameAs(response);
            assertThat(response.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("throws when the track does not exist")
        void shouldThrowWhenMissing() {
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.get(TRACK_ID))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verify(accessService, never()).isHrOrAdmin();
            verifyNoInteractions(trackMapper);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves a draft track with a trimmed code and the current user")
        void shouldCreateDraftTrack() {
            authenticate();
            CareerTrackCreateRequestDto request = createRequest("  " + CODE + "  ");
            CareerTrack mapped = track(CareerTrackStatus.PUBLISHED);
            mapped.setCode("from-mapper");
            when(trackRepository.existsByCodeIgnoreCase("  " + CODE + "  ")).thenReturn(false);
            when(trackMapper.toEntity(request)).thenReturn(mapped);
            stubSave();
            CareerTrackResponseDto response = stubEmptyGraph();

            CareerTrackResponseDto result = careerTrackService.create(request);

            assertThat(result).isSameAs(response);
            assertThat(result.getStatus()).isEqualTo(CareerTrackStatus.DRAFT);
            assertThat(result.getCode()).isEqualTo(CODE);

            ArgumentCaptor<CareerTrack> captor = ArgumentCaptor.forClass(CareerTrack.class);
            verify(trackRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo(CODE);
            assertThat(captor.getValue().getStatus()).isEqualTo(CareerTrackStatus.DRAFT);
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getName()).isEqualTo(NAME);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("rejects a duplicate code")
        void shouldRejectDuplicateCode() {
            CareerTrackCreateRequestDto request = createRequest("  " + CODE + "  ");
            when(trackRepository.existsByCodeIgnoreCase("  " + CODE + "  ")).thenReturn(true);

            assertThatThrownBy(() -> careerTrackService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("  " + CODE + "  ");

            verify(trackRepository, never()).save(any());
            verifyNoInteractions(trackMapper);
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.create(createRequest(CODE)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(trackRepository, trackMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("trims a changed code after the uniqueness check")
        void shouldTrimChangedCode() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            CareerTrackUpdateRequestDto request = CareerTrackUpdateRequestDto.builder()
                    .code("  NEW-CODE  ")
                    .name("Platform")
                    .description("Updated")
                    .type(CareerTrackType.EXPERT)
                    .build();
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
            when(trackRepository.existsByCodeIgnoreCaseAndIdNot("  NEW-CODE  ", TRACK_ID)).thenReturn(false);
            applyTrackUpdate();
            stubSaveAndFlush();
            stubEmptyGraph();

            CareerTrackResponseDto result = careerTrackService.update(TRACK_ID, request);

            assertThat(track.getCode()).isEqualTo("NEW-CODE");
            assertThat(track.getName()).isEqualTo("Platform");
            assertThat(track.getDescription()).isEqualTo("Updated");
            assertThat(track.getType()).isEqualTo(CareerTrackType.EXPERT);
            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.DRAFT);
            assertThat(result.getCode()).isEqualTo("NEW-CODE");
        }

        @Test
        @DisplayName("keeps the code when the request omits it")
        void shouldKeepCodeWhenOmitted() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            CareerTrackUpdateRequestDto request = CareerTrackUpdateRequestDto.builder()
                    .name("Platform")
                    .build();
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
            applyTrackUpdate();
            stubSaveAndFlush();
            stubEmptyGraph();

            careerTrackService.update(TRACK_ID, request);

            assertThat(track.getCode()).isEqualTo(CODE);
            assertThat(track.getName()).isEqualTo("Platform");
            verify(trackRepository, never()).existsByCodeIgnoreCaseAndIdNot(anyString(), any());
        }

        @Test
        @DisplayName("skips the uniqueness check for a blank code and stores the trimmed value")
        void shouldSkipUniquenessCheckForBlankCode() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            CareerTrackUpdateRequestDto request = CareerTrackUpdateRequestDto.builder()
                    .code("   ")
                    .build();
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
            applyTrackUpdate();
            stubSaveAndFlush();
            stubEmptyGraph();

            careerTrackService.update(TRACK_ID, request);

            assertThat(track.getCode()).isEmpty();
            verify(trackRepository, never()).existsByCodeIgnoreCaseAndIdNot(anyString(), any());
        }

        @Test
        @DisplayName("rejects a duplicate code")
        void shouldRejectDuplicateCode() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            CareerTrackUpdateRequestDto request = CareerTrackUpdateRequestDto.builder()
                    .code("TAKEN")
                    .build();
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
            when(trackRepository.existsByCodeIgnoreCaseAndIdNot("TAKEN", TRACK_ID)).thenReturn(true);

            assertThatThrownBy(() -> careerTrackService.update(TRACK_ID, request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("TAKEN");

            verify(trackMapper, never()).updateEntity(any(), any());
            verify(trackRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @EnumSource(value = CareerTrackStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
        @DisplayName("rejects changes to a track that is not a draft")
        void shouldRejectNonDraft(CareerTrackStatus status) {
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track(status)));

            assertThatThrownBy(() -> careerTrackService.update(
                    TRACK_ID,
                    CareerTrackUpdateRequestDto.builder().name("Platform").build()
            )).isInstanceOf(ImmutableResourceException.class);

            verify(trackRepository, never()).existsByCodeIgnoreCaseAndIdNot(anyString(), any());
            verifyNoInteractions(trackMapper);
        }

        @Test
        @DisplayName("throws when the track does not exist")
        void shouldThrowWhenMissing() {
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.update(
                    TRACK_ID,
                    CareerTrackUpdateRequestDto.builder().name("Platform").build()
            ))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verifyNoInteractions(trackMapper);
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.update(
                    TRACK_ID,
                    CareerTrackUpdateRequestDto.builder().name("Platform").build()
            )).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(trackRepository);
        }
    }

    @Nested
    @DisplayName("addNode")
    class AddNode {

        @Test
        @DisplayName("saves an entry-point node on a draft track")
        void shouldAddEntryPointNode() {
            arrangeDraftTrack();
            when(nodeRepository.existsByTrackIdAndPositionIdAndGradeId(TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(false);
            when(nodeRepository.save(any(CareerTrackNode.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            CareerTrackNodeResponseDto expected = CareerTrackNodeResponseDto.builder().id(NODE_ID).build();
            when(nodeMapper.toResponseDto(any(CareerTrackNode.class))).thenReturn(expected);

            CareerTrackNodeResponseDto result = careerTrackService.addNode(TRACK_ID, nodeRequest(true));

            assertThat(result).isSameAs(expected);
            CareerTrackNode saved = captureSavedNode();
            assertThat(saved.getTrackId()).isEqualTo(TRACK_ID);
            assertThat(saved.getPositionId()).isEqualTo(POSITION_ID);
            assertThat(saved.getGradeId()).isEqualTo(GRADE_ID);
            assertThat(saved.getLabel()).isEqualTo("Senior");
            assertThat(saved.getDescription()).isEqualTo("Senior node");
            assertThat(saved.getDisplayOrder()).isEqualTo(3);
            assertThat(saved.getIsEntryPoint()).isTrue();
            verify(employeeDirectory).requirePosition(POSITION_ID);
            verify(employeeDirectory).requireGrade(GRADE_ID);
        }

        @Test
        @DisplayName("stores a missing entry-point flag as false")
        void shouldStoreMissingEntryPointAsFalse() {
            arrangeDraftTrack();
            when(nodeRepository.existsByTrackIdAndPositionIdAndGradeId(TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(false);
            when(nodeRepository.save(any(CareerTrackNode.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(nodeMapper.toResponseDto(any(CareerTrackNode.class)))
                    .thenReturn(CareerTrackNodeResponseDto.builder().build());

            careerTrackService.addNode(TRACK_ID, nodeRequest(null));

            assertThat(captureSavedNode().getIsEntryPoint()).isFalse();
        }

        @Test
        @DisplayName("rejects a duplicate position and grade")
        void shouldRejectDuplicateNode() {
            arrangeDraftTrack();
            when(nodeRepository.existsByTrackIdAndPositionIdAndGradeId(TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> careerTrackService.addNode(TRACK_ID, nodeRequest(false)))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(nodeRepository, never()).save(any());
            verifyNoInteractions(nodeMapper);
        }

        @Test
        @DisplayName("rejects a node on a published track")
        void shouldRejectPublishedTrack() {
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));

            assertThatThrownBy(() -> careerTrackService.addNode(TRACK_ID, nodeRequest(true)))
                    .isInstanceOf(ImmutableResourceException.class);

            verifyNoInteractions(employeeDirectory, nodeMapper);
            verify(nodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the track does not exist")
        void shouldThrowWhenTrackMissing() {
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.addNode(TRACK_ID, nodeRequest(true)))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.addNode(TRACK_ID, nodeRequest(true)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(trackRepository, employeeDirectory);
        }
    }

    @Nested
    @DisplayName("updateNode")
    class UpdateNode {

        @Test
        @DisplayName("updates the label without checking position or grade")
        void shouldUpdateLabelOnly() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 1);
            arrangeNodeOnDraft(node);
            applyNodeUpdate();
            stubNodeSaveAndFlush();

            CareerTrackNodeResponseDto result = careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().label("Renamed").displayOrder(4).build()
            );

            assertThat(result.getId()).isEqualTo(NODE_ID);
            assertThat(node.getLabel()).isEqualTo("Renamed");
            assertThat(node.getDisplayOrder()).isEqualTo(4);
            assertThat(node.getPositionId()).isEqualTo(POSITION_ID);
            verify(employeeDirectory, never()).requirePosition(any());
            verify(employeeDirectory, never()).requireGrade(any());
            verify(nodeRepository, never()).existsByTrackIdAndPositionIdAndGradeIdAndIdNot(
                    any(), any(), any(), any()
            );
        }

        @Test
        @DisplayName("checks the new position against the current grade")
        void shouldUpdatePositionOnly() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 1);
            arrangeNodeOnDraft(node);
            when(nodeRepository.existsByTrackIdAndPositionIdAndGradeIdAndIdNot(
                    TRACK_ID, OTHER_POSITION_ID, GRADE_ID, NODE_ID
            )).thenReturn(false);
            applyNodeUpdate();
            stubNodeSaveAndFlush();

            careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().positionId(OTHER_POSITION_ID).build()
            );

            assertThat(node.getPositionId()).isEqualTo(OTHER_POSITION_ID);
            assertThat(node.getGradeId()).isEqualTo(GRADE_ID);
            verify(employeeDirectory).requirePosition(OTHER_POSITION_ID);
            verify(employeeDirectory, never()).requireGrade(any());
        }

        @Test
        @DisplayName("checks the new grade against the current position")
        void shouldUpdateGradeOnly() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 1);
            arrangeNodeOnDraft(node);
            when(nodeRepository.existsByTrackIdAndPositionIdAndGradeIdAndIdNot(
                    TRACK_ID, POSITION_ID, OTHER_GRADE_ID, NODE_ID
            )).thenReturn(false);
            applyNodeUpdate();
            stubNodeSaveAndFlush();

            careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().gradeId(OTHER_GRADE_ID).build()
            );

            assertThat(node.getGradeId()).isEqualTo(OTHER_GRADE_ID);
            verify(employeeDirectory).requireGrade(OTHER_GRADE_ID);
            verify(employeeDirectory, never()).requirePosition(any());
        }

        @Test
        @DisplayName("rejects a duplicate position and grade")
        void shouldRejectDuplicatePositionAndGrade() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 1);
            arrangeNodeOnDraft(node);
            when(nodeRepository.existsByTrackIdAndPositionIdAndGradeIdAndIdNot(
                    TRACK_ID, OTHER_POSITION_ID, GRADE_ID, NODE_ID
            )).thenReturn(true);

            assertThatThrownBy(() -> careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().positionId(OTHER_POSITION_ID).build()
            )).isInstanceOf(DuplicateResourceException.class);

            verify(nodeMapper, never()).updateEntity(any(), any());
            verify(nodeRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("rejects a node on a published track")
        void shouldRejectPublishedTrack() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 1);
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));

            assertThatThrownBy(() -> careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().label("Renamed").build()
            )).isInstanceOf(ImmutableResourceException.class);

            verifyNoInteractions(employeeDirectory, nodeMapper);
        }

        @Test
        @DisplayName("throws when the node does not exist")
        void shouldThrowWhenNodeMissing() {
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().label("Renamed").build()
            ))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(NODE_ID.toString());

            verifyNoInteractions(trackRepository);
        }

        @Test
        @DisplayName("throws when the owning track does not exist")
        void shouldThrowWhenTrackMissing() {
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(NODE_ID, TRACK_ID, 1)));
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().label("Renamed").build()
            ))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.updateNode(
                    NODE_ID,
                    CareerTrackNodeUpdateRequestDto.builder().label("Renamed").build()
            )).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(nodeRepository, trackRepository);
        }
    }

    @Nested
    @DisplayName("deleteNode")
    class DeleteNode {

        @Test
        @DisplayName("deletes transitions that start or end at the node, then the node")
        void shouldDeleteNodeAndRelatedTransitions() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 1);
            CareerTransition outgoing = transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID);
            CareerTransition incoming = transition(OTHER_TRANSITION_ID, OTHER_NODE_ID, NODE_ID);
            arrangeNodeOnDraft(node);
            when(transitionRepository.findAllBySourceNodeId(NODE_ID)).thenReturn(List.of(outgoing));
            when(transitionRepository.findAllByTargetNodeId(NODE_ID)).thenReturn(List.of(incoming));

            careerTrackService.deleteNode(NODE_ID);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<CareerTransition>> captor = ArgumentCaptor.forClass(List.class);
            verify(transitionRepository).deleteAll(captor.capture());
            assertThat(captor.getValue()).containsExactly(outgoing, incoming);
            verify(nodeRepository).delete(node);
        }

        @Test
        @DisplayName("rejects deletion from a published track")
        void shouldRejectPublishedTrack() {
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(NODE_ID, TRACK_ID, 1)));
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));

            assertThatThrownBy(() -> careerTrackService.deleteNode(NODE_ID))
                    .isInstanceOf(ImmutableResourceException.class);

            verify(transitionRepository, never()).findAllBySourceNodeId(any());
            verify(nodeRepository, never()).delete(any(CareerTrackNode.class));
        }

        @Test
        @DisplayName("throws when the node does not exist")
        void shouldThrowWhenNodeMissing() {
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.deleteNode(NODE_ID))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(NODE_ID.toString());

            verifyNoInteractions(trackRepository, transitionRepository);
        }

        @Test
        @DisplayName("throws when the owning track does not exist")
        void shouldThrowWhenTrackMissing() {
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(NODE_ID, TRACK_ID, 1)));
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.deleteNode(NODE_ID))
                    .isInstanceOf(CareerTrackNotFoundException.class);

            verifyNoInteractions(transitionRepository);
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.deleteNode(NODE_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(nodeRepository);
        }
    }

    @Nested
    @DisplayName("addTransition")
    class AddTransition {

        @Test
        @DisplayName("saves an active transition between two nodes of the draft track")
        void shouldAddActiveTransition() {
            CareerTrackNode source = node(NODE_ID, TRACK_ID, 0);
            CareerTrackNode target = node(OTHER_NODE_ID, TRACK_ID, 1);
            arrangeDraftTransition(source, target);
            when(transitionRepository.existsBySourceNodeIdAndTargetNodeId(NODE_ID, OTHER_NODE_ID)).thenReturn(false);
            when(transitionRepository.save(any(CareerTransition.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            CareerTransitionResponseDto expected = CareerTransitionResponseDto.builder().id(TRANSITION_ID).build();
            when(transitionMapper.toResponseDto(any(CareerTransition.class))).thenReturn(expected);

            CareerTransitionResponseDto result = careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            );

            assertThat(result).isSameAs(expected);
            CareerTransition saved = captureSavedTransition();
            assertThat(saved.getSourceNodeId()).isEqualTo(NODE_ID);
            assertThat(saved.getTargetNodeId()).isEqualTo(OTHER_NODE_ID);
            assertThat(saved.getTransitionType()).isEqualTo("PROMOTION");
            assertThat(saved.getDescription()).isEqualTo("Promote");
            assertThat(saved.getEstimatedMonths()).isEqualTo(12);
            assertThat(saved.getActive()).isTrue();
        }

        @Test
        @DisplayName("treats a missing active flag as active")
        void shouldDefaultMissingActiveFlagToTrue() {
            arrangeDraftTransition(node(NODE_ID, TRACK_ID, 0), node(OTHER_NODE_ID, TRACK_ID, 1));
            when(transitionRepository.existsBySourceNodeIdAndTargetNodeId(NODE_ID, OTHER_NODE_ID)).thenReturn(false);
            when(transitionRepository.save(any(CareerTransition.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transitionMapper.toResponseDto(any(CareerTransition.class)))
                    .thenReturn(CareerTransitionResponseDto.builder().build());

            careerTrackService.addTransition(TRACK_ID, transitionRequest(NODE_ID, OTHER_NODE_ID, null));

            assertThat(captureSavedTransition().getActive()).isTrue();
        }

        @Test
        @DisplayName("stores an explicitly inactive transition")
        void shouldStoreInactiveTransition() {
            arrangeDraftTransition(node(NODE_ID, TRACK_ID, 0), node(OTHER_NODE_ID, TRACK_ID, 1));
            when(transitionRepository.existsBySourceNodeIdAndTargetNodeId(NODE_ID, OTHER_NODE_ID)).thenReturn(false);
            when(transitionRepository.save(any(CareerTransition.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(transitionMapper.toResponseDto(any(CareerTransition.class)))
                    .thenReturn(CareerTransitionResponseDto.builder().build());

            careerTrackService.addTransition(TRACK_ID, transitionRequest(NODE_ID, OTHER_NODE_ID, false));

            assertThat(captureSavedTransition().getActive()).isFalse();
        }

        @Test
        @DisplayName("rejects a transition whose source belongs to another track")
        void shouldRejectForeignSourceNode() {
            arrangeDraftTransition(node(NODE_ID, OTHER_TRACK_ID, 0), node(OTHER_NODE_ID, TRACK_ID, 1));

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            ))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verify(transitionRepository, never()).existsBySourceNodeIdAndTargetNodeId(any(), any());
        }

        @Test
        @DisplayName("rejects a transition whose target belongs to another track")
        void shouldRejectForeignTargetNode() {
            arrangeDraftTransition(node(NODE_ID, TRACK_ID, 0), node(OTHER_NODE_ID, OTHER_TRACK_ID, 1));

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            ))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(TRACK_ID.toString());
        }

        @Test
        @DisplayName("rejects a transition from a node to itself")
        void shouldRejectSelfLoop() {
            CareerTrackNode node = node(NODE_ID, TRACK_ID, 0);
            arrangeDraftTransition(node, node);

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, NODE_ID, true)
            )).isInstanceOf(CyclicCareerTrackException.class);

            verify(transitionRepository, never()).existsBySourceNodeIdAndTargetNodeId(any(), any());
            verify(transitionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a duplicate transition")
        void shouldRejectDuplicateTransition() {
            arrangeDraftTransition(node(NODE_ID, TRACK_ID, 0), node(OTHER_NODE_ID, TRACK_ID, 1));
            when(transitionRepository.existsBySourceNodeIdAndTargetNodeId(NODE_ID, OTHER_NODE_ID)).thenReturn(true);

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            )).isInstanceOf(DuplicateResourceException.class);

            verify(transitionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a transition on a published track")
        void shouldRejectPublishedTrack() {
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            )).isInstanceOf(ImmutableResourceException.class);

            verify(nodeRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws when the source node does not exist")
        void shouldThrowWhenSourceMissing() {
            arrangeDraftTrack();
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            ))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(NODE_ID.toString());

            verify(nodeRepository, never()).findById(OTHER_NODE_ID);
        }

        @Test
        @DisplayName("throws when the target node does not exist")
        void shouldThrowWhenTargetMissing() {
            arrangeDraftTrack();
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(NODE_ID, TRACK_ID, 0)));
            when(nodeRepository.findById(OTHER_NODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            ))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(OTHER_NODE_ID.toString());
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.addTransition(
                    TRACK_ID,
                    transitionRequest(NODE_ID, OTHER_NODE_ID, true)
            )).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(trackRepository, nodeRepository);
        }
    }

    @Nested
    @DisplayName("deleteTransition")
    class DeleteTransition {

        @Test
        @DisplayName("deletes a transition of a draft track")
        void shouldDeleteTransition() {
            CareerTransition transition = transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID);
            when(transitionRepository.findById(TRANSITION_ID)).thenReturn(Optional.of(transition));
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(NODE_ID, TRACK_ID, 0)));
            arrangeDraftTrack();

            careerTrackService.deleteTransition(TRANSITION_ID);

            verify(transitionRepository).delete(transition);
        }

        @Test
        @DisplayName("rejects deletion from a published track")
        void shouldRejectPublishedTrack() {
            when(transitionRepository.findById(TRANSITION_ID))
                    .thenReturn(Optional.of(transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID)));
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node(NODE_ID, TRACK_ID, 0)));
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));

            assertThatThrownBy(() -> careerTrackService.deleteTransition(TRANSITION_ID))
                    .isInstanceOf(ImmutableResourceException.class);

            verify(transitionRepository, never()).delete(any(CareerTransition.class));
        }

        @Test
        @DisplayName("throws when the transition does not exist")
        void shouldThrowWhenMissing() {
            when(transitionRepository.findById(TRANSITION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.deleteTransition(TRANSITION_ID))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(TRANSITION_ID.toString());

            verifyNoInteractions(nodeRepository, trackRepository);
        }

        @Test
        @DisplayName("throws when the source node does not exist")
        void shouldThrowWhenSourceNodeMissing() {
            when(transitionRepository.findById(TRANSITION_ID))
                    .thenReturn(Optional.of(transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID)));
            when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.deleteTransition(TRANSITION_ID))
                    .isInstanceOf(CareerTrackStructureNotFoundException.class)
                    .hasMessageContaining(NODE_ID.toString());

            verify(transitionRepository, never()).delete(any(CareerTransition.class));
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.deleteTransition(TRANSITION_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(transitionRepository);
        }
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("publishes the first revision of an acyclic draft and emits an outbox event")
        void shouldPublishFirstRevision() throws JacksonException {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            CareerTrackNode source = node(NODE_ID, TRACK_ID, 0);
            CareerTrackNode target = node(OTHER_NODE_ID, TRACK_ID, 1);
            CareerTransition edge = transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID);
            arrangePublish(track, List.of(source, target), List.of(edge), Optional.empty(), Optional.empty());

            CareerTrackResponseDto result = careerTrackService.publish(TRACK_ID, publication(VERSION));

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
            assertThat(result.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
            assertThat(result.getNodes()).extracting(CareerTrackNodeResponseDto::getId)
                    .containsExactly(NODE_ID, OTHER_NODE_ID);

            ArgumentCaptor<CareerTrackRevision> revisionCaptor = ArgumentCaptor.forClass(CareerTrackRevision.class);
            verify(revisionRepository).save(revisionCaptor.capture());
            CareerTrackRevision revision = revisionCaptor.getValue();
            assertThat(revision.getTrackId()).isEqualTo(TRACK_ID);
            assertThat(revision.getRevision()).isEqualTo(1);
            assertThat(revision.getStatus()).isEqualTo(CareerRevisionStatus.PUBLISHED);
            assertThat(revision.getSnapshot()).isEqualTo("{}");
            assertThat(revision.getPublishedBy()).isEqualTo(USER_ID);
            assertThat(revision.getPublishedAt()).isNotNull();
            verify(revisionRepository, never()).saveAndFlush(any());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> snapshotCaptor = ArgumentCaptor.forClass(Map.class);
            verify(objectMapper).writeValueAsString(snapshotCaptor.capture());
            Map<String, Object> snapshot = snapshotCaptor.getValue();
            assertThat(snapshot).containsEntry("code", CODE);
            assertThat(snapshot).containsEntry("name", NAME);
            assertThat(snapshot).containsEntry("type", CareerTrackType.VERTICAL);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) snapshot.get("nodes");
            assertThat(nodes).hasSize(2);
            assertThat(nodes.get(0))
                    .containsEntry("id", NODE_ID)
                    .containsEntry("positionId", POSITION_ID)
                    .containsEntry("gradeId", GRADE_ID)
                    .containsEntry("label", "L0")
                    .containsEntry("displayOrder", 0)
                    .containsEntry("entryPoint", true);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transitions = (List<Map<String, Object>>) snapshot.get("transitions");
            assertThat(transitions).containsExactly(Map.of(
                    "sourceNodeId", NODE_ID,
                    "targetNodeId", OTHER_NODE_ID,
                    "transitionType", "PROMOTION",
                    "active", true
            ));

            verify(outboxEventService).saveEvent(
                    eq(TRACK_ID),
                    eq(KafkaEventConstants.AGGREGATE_TYPE_CAREER_TRACK),
                    eq(KafkaEventConstants.CAREER_TRACK_PUBLISHED),
                    eq(KafkaEventConstants.EVENT_VERSION_V1),
                    eq(Map.of("trackId", TRACK_ID, "code", CODE, "revision", 1))
            );
        }

        @Test
        @DisplayName("archives the previous published revision and increments the number")
        void shouldArchivePreviousRevisionWhenRepublishing() {
            CareerTrack track = track(CareerTrackStatus.PUBLISHED);
            CareerTrackRevision latest = revision(REVISION_ID, 2, CareerRevisionStatus.ARCHIVED);
            CareerTrackRevision previous = revision(PREVIOUS_REVISION_ID, 2, CareerRevisionStatus.PUBLISHED);
            arrangePublish(
                    track,
                    List.of(node(NODE_ID, TRACK_ID, 0)),
                    List.of(),
                    Optional.of(latest),
                    Optional.of(previous)
            );

            careerTrackService.publish(TRACK_ID, publication(VERSION));

            assertThat(previous.getStatus()).isEqualTo(CareerRevisionStatus.ARCHIVED);
            verify(revisionRepository).saveAndFlush(previous);

            ArgumentCaptor<CareerTrackRevision> revisionCaptor = ArgumentCaptor.forClass(CareerTrackRevision.class);
            verify(revisionRepository).save(revisionCaptor.capture());
            assertThat(revisionCaptor.getValue().getRevision()).isEqualTo(3);
            assertThat(revisionCaptor.getValue().getStatus()).isEqualTo(CareerRevisionStatus.PUBLISHED);
            verify(outboxEventService).saveEvent(
                    eq(TRACK_ID),
                    eq(KafkaEventConstants.AGGREGATE_TYPE_CAREER_TRACK),
                    eq(KafkaEventConstants.CAREER_TRACK_PUBLISHED),
                    eq(KafkaEventConstants.EVENT_VERSION_V1),
                    eq(Map.of("trackId", TRACK_ID, "code", CODE, "revision", 3))
            );
        }

        @Test
        @DisplayName("publishes a diamond graph whose shared target was already visited")
        void shouldPublishDiamondWithoutCycle() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            arrangePublish(
                    track,
                    List.of(
                            node(NODE_ID, TRACK_ID, 0),
                            node(OTHER_NODE_ID, TRACK_ID, 1),
                            node(THIRD_NODE_ID, TRACK_ID, 2),
                            node(FOURTH_NODE_ID, TRACK_ID, 3)
                    ),
                    List.of(
                            transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID),
                            transition(OTHER_TRANSITION_ID, NODE_ID, THIRD_NODE_ID),
                            transition(UUID.fromString("12121212-1212-1212-1212-121212121212"), OTHER_NODE_ID, FOURTH_NODE_ID),
                            transition(UUID.fromString("13131313-1313-1313-1313-131313131313"), THIRD_NODE_ID, FOURTH_NODE_ID)
                    ),
                    Optional.empty(),
                    Optional.empty()
            );

            careerTrackService.publish(TRACK_ID, publication(VERSION));

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
        }

        @Test
        @DisplayName("ignores an edge that leaves the node set and an edge that starts outside it")
        void shouldIgnoreEdgesOutsideTheNodeSet() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            UUID outsideSource = UUID.fromString("67676767-6767-6767-6767-676767676767");
            arrangePublish(
                    track,
                    List.of(node(NODE_ID, TRACK_ID, 0)),
                    List.of(
                            transition(TRANSITION_ID, NODE_ID, FOREIGN_NODE_ID),
                            transition(OTHER_TRANSITION_ID, outsideSource, NODE_ID)
                    ),
                    Optional.empty(),
                    Optional.empty()
            );

            careerTrackService.publish(TRACK_ID, publication(VERSION));

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
        }

        @Test
        @DisplayName("rejects a self-loop while publishing")
        void shouldRejectSelfLoop() {
            arrangeStructure(
                    track(CareerTrackStatus.DRAFT),
                    List.of(node(NODE_ID, TRACK_ID, 0)),
                    List.of(transition(TRANSITION_ID, NODE_ID, NODE_ID))
            );

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(CyclicCareerTrackException.class);

            assertPublishStoppedBeforeSnapshot();
        }

        @Test
        @DisplayName("rejects a cycle through several nodes")
        void shouldRejectCycle() {
            arrangeStructure(
                    track(CareerTrackStatus.DRAFT),
                    List.of(
                            node(NODE_ID, TRACK_ID, 0),
                            node(OTHER_NODE_ID, TRACK_ID, 1),
                            node(THIRD_NODE_ID, TRACK_ID, 2)
                    ),
                    List.of(
                            transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID),
                            transition(OTHER_TRANSITION_ID, OTHER_NODE_ID, THIRD_NODE_ID),
                            transition(UUID.fromString("14141414-1414-1414-1414-141414141414"), THIRD_NODE_ID, NODE_ID)
                    )
            );

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(CyclicCareerTrackException.class);

            assertPublishStoppedBeforeSnapshot();
        }

        @Test
        @DisplayName("rejects an empty track")
        void shouldRejectEmptyTrack() {
            arrangeStructure(track(CareerTrackStatus.DRAFT), List.of(), List.of());

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Пустой карьерный трек не может быть опубликован");

            verify(transitionRepository, never()).findAllBySourceNodeIdIn(any());
            verify(revisionRepository, never()).findTopByTrackIdOrderByRevisionDesc(any());
            verifyNoInteractions(objectMapper, outboxEventService);
        }

        @Test
        @DisplayName("rejects an archived track before checking the version")
        void shouldRejectArchivedTrackBeforeVersionCheck() {
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.ARCHIVED)));

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(9L)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(CareerTrackStatus.ARCHIVED.name())
                    .hasMessageContaining(CareerTrackStatus.PUBLISHED.name());

            verify(nodeRepository, never()).findAllByTrackIdOrderByDisplayOrderAsc(any());
        }

        @Test
        @DisplayName("rejects a stale expected version before loading nodes")
        void shouldRejectStaleVersion() {
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.DRAFT)));

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(9L)))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessage("Ожидаемая версия карьерного трека не совпадает с текущей");

            verify(nodeRepository, never()).findAllByTrackIdOrderByDisplayOrderAsc(any());
        }

        @Test
        @DisplayName("publishes when the request omits the expected version")
        void shouldPublishWhenRequestIsMissing() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            arrangePublish(
                    track,
                    List.of(node(NODE_ID, TRACK_ID, 0)),
                    List.of(),
                    Optional.empty(),
                    Optional.empty()
            );

            careerTrackService.publish(TRACK_ID, null);

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
        }

        @Test
        @DisplayName("publishes when the expected version is null")
        void shouldPublishWhenExpectedVersionIsNull() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            arrangePublish(
                    track,
                    List.of(node(NODE_ID, TRACK_ID, 0)),
                    List.of(),
                    Optional.empty(),
                    Optional.empty()
            );

            careerTrackService.publish(TRACK_ID, CareerTrackPublicationRequestDto.builder().build());

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.PUBLISHED);
        }

        @Test
        @DisplayName("wraps a snapshot serialization failure")
        void shouldWrapSnapshotFailure() throws JacksonException {
            authenticate();
            arrangeStructure(
                    track(CareerTrackStatus.DRAFT),
                    List.of(node(NODE_ID, TRACK_ID, 0)),
                    List.of()
            );
            when(revisionRepository.findTopByTrackIdOrderByRevisionDesc(TRACK_ID)).thenReturn(Optional.empty());
            when(revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                    TRACK_ID,
                    CareerRevisionStatus.PUBLISHED
            )).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenThrow(new JacksonException("fail") {
            });

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Не удалось сформировать снимок карьерного трека")
                    .hasCauseInstanceOf(JacksonException.class);

            verify(revisionRepository, never()).save(any());
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("throws when the track does not exist")
        void shouldThrowWhenMissing() {
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.publish(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(trackRepository);
        }
    }

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("archives a published track and its current revision")
        void shouldArchivePublishedTrackAndRevision() {
            CareerTrack track = track(CareerTrackStatus.PUBLISHED);
            CareerTrackRevision previous = revision(PREVIOUS_REVISION_ID, 2, CareerRevisionStatus.PUBLISHED);
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
            stubSaveAndFlush();
            CareerTrackResponseDto response = stubTrackMapperAndEmptyNodes();
            when(revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                    TRACK_ID,
                    CareerRevisionStatus.PUBLISHED
            )).thenReturn(Optional.of(previous), Optional.empty());

            CareerTrackResponseDto result = careerTrackService.archive(TRACK_ID, publication(VERSION));

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.ARCHIVED);
            assertThat(previous.getStatus()).isEqualTo(CareerRevisionStatus.ARCHIVED);
            assertThat(result).isSameAs(response);
            assertThat(result.getStatus()).isEqualTo(CareerTrackStatus.ARCHIVED);
            verify(revisionRepository).saveAndFlush(previous);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("archives a published track that has no published revision")
        void shouldArchiveWhenNoPublishedRevisionExists() {
            CareerTrack track = track(CareerTrackStatus.PUBLISHED);
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
            stubSaveAndFlush();
            stubEmptyGraph();

            careerTrackService.archive(TRACK_ID, null);

            assertThat(track.getStatus()).isEqualTo(CareerTrackStatus.ARCHIVED);
            verify(revisionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("rejects a stale expected version")
        void shouldRejectStaleVersion() {
            when(trackRepository.findByIdForUpdate(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));

            assertThatThrownBy(() -> careerTrackService.archive(TRACK_ID, publication(9L)))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessage("Ожидаемая версия карьерного трека не совпадает с текущей");

            verify(revisionRepository, never()).findFirstByTrackIdAndStatusOrderByRevisionDesc(any(), any());
        }

        @ParameterizedTest
        @EnumSource(value = CareerTrackStatus.class, names = {"DRAFT", "ARCHIVED"})
        @DisplayName("rejects a track that is not published, before the version check")
        void shouldRejectTrackThatIsNotPublished(CareerTrackStatus status) {
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track(status)));

            assertThatThrownBy(() -> careerTrackService.archive(TRACK_ID, publication(9L)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(status.name())
                    .hasMessageContaining(CareerTrackStatus.ARCHIVED.name());

            verifyNoInteractions(revisionRepository);
        }

        @Test
        @DisplayName("throws when the track does not exist")
        void shouldThrowWhenMissing() {
            when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.archive(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> careerTrackService.archive(TRACK_ID, publication(VERSION)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(trackRepository);
        }
    }

    @Nested
    @DisplayName("availableTracks")
    class AvailableTracks {

        @Test
        @DisplayName("returns published tracks whose node matches the employee")
        void shouldReturnMatchingTracks() {
            CareerTrack matching = track(CareerTrackStatus.PUBLISHED);
            CareerTrack other = track(CareerTrackStatus.PUBLISHED);
            other.setId(OTHER_TRACK_ID);
            other.setCode("OTHER");
            other.setName("Other");
            CareerTrackNode current = node(NODE_ID, TRACK_ID, 0);
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID))
                    .thenReturn(employee(POSITION_ID, GRADE_ID));
            when(trackRepository.findAllByStatus(CareerTrackStatus.PUBLISHED)).thenReturn(List.of(matching, other));
            when(nodeRepository.findByTrackIdAndPositionIdAndGradeId(TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.of(current));
            when(nodeRepository.findByTrackIdAndPositionIdAndGradeId(OTHER_TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.empty());
            when(transitionRepository.findAllBySourceNodeIdAndActiveTrue(NODE_ID))
                    .thenReturn(List.of(
                            transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID),
                            transition(OTHER_TRANSITION_ID, NODE_ID, THIRD_NODE_ID)
                    ));

            var result = careerTrackService.availableTracks(EMPLOYEE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTrackId()).isEqualTo(TRACK_ID);
            assertThat(result.get(0).getCode()).isEqualTo(CODE);
            assertThat(result.get(0).getName()).isEqualTo(NAME);
            assertThat(result.get(0).getType()).isEqualTo(CareerTrackType.VERTICAL);
            assertThat(result.get(0).getCurrentNodeId()).isEqualTo(NODE_ID);
            assertThat(result.get(0).getAvailableTargetNodeIds()).containsExactly(OTHER_NODE_ID, THIRD_NODE_ID);
            verify(accessService).requireSelfOrManager(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("returns an empty list when the employee has no position")
        void shouldReturnEmptyWhenPositionMissing() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(null, GRADE_ID));

            assertThat(careerTrackService.availableTracks(EMPLOYEE_ID)).isEmpty();

            verify(trackRepository, never()).findAllByStatus(any());
        }

        @Test
        @DisplayName("returns an empty list when the employee has no grade")
        void shouldReturnEmptyWhenGradeMissing() {
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(POSITION_ID, null));

            assertThat(careerTrackService.availableTracks(EMPLOYEE_ID)).isEmpty();

            verify(trackRepository, never()).findAllByStatus(any());
        }

        @Test
        @DisplayName("denies a user who cannot see the employee")
        void shouldDenyWithoutAccess() {
            denyEmployee(EMPLOYEE_ID);

            assertThatThrownBy(() -> careerTrackService.availableTracks(EMPLOYEE_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(employeeDirectory, trackRepository);
        }
    }

    @Nested
    @DisplayName("availableTargets")
    class AvailableTargets {

        @Test
        @DisplayName("returns active targets and drops a transition whose node is missing")
        void shouldReturnActiveTargets() {
            when(trackRepository.findById(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID))
                    .thenReturn(employee(POSITION_ID, GRADE_ID));
            CareerTrackNode current = node(NODE_ID, TRACK_ID, 0);
            CareerTrackNode target = node(OTHER_NODE_ID, TRACK_ID, 1);
            target.setLabel("Staff");
            when(nodeRepository.findByTrackIdAndPositionIdAndGradeId(TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.of(current));
            when(nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(TRACK_ID))
                    .thenReturn(List.of(current, target));
            CareerTransition reachable = transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID);
            reachable.setEstimatedMonths(9);
            when(transitionRepository.findAllBySourceNodeIdAndActiveTrue(NODE_ID))
                    .thenReturn(List.of(reachable, transition(OTHER_TRANSITION_ID, NODE_ID, FOREIGN_NODE_ID)));

            AvailableTargetsResponseDto result = careerTrackService.availableTargets(TRACK_ID, EMPLOYEE_ID);

            assertThat(result.getTrackId()).isEqualTo(TRACK_ID);
            assertThat(result.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(result.getCurrentNodeId()).isEqualTo(NODE_ID);
            assertThat(result.getTargets()).hasSize(1);
            assertThat(result.getTargets().get(0).getNodeId()).isEqualTo(OTHER_NODE_ID);
            assertThat(result.getTargets().get(0).getPositionId()).isEqualTo(POSITION_ID);
            assertThat(result.getTargets().get(0).getGradeId()).isEqualTo(GRADE_ID);
            assertThat(result.getTargets().get(0).getLabel()).isEqualTo("Staff");
            assertThat(result.getTargets().get(0).getTransitionType()).isEqualTo("PROMOTION");
            assertThat(result.getTargets().get(0).getEstimatedMonths()).isEqualTo(9);
        }

        @Test
        @DisplayName("returns no targets when the employee is not on the track")
        void shouldReturnEmptyWhenCurrentNodeMissing() {
            when(trackRepository.findById(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID))
                    .thenReturn(employee(POSITION_ID, GRADE_ID));
            when(nodeRepository.findByTrackIdAndPositionIdAndGradeId(TRACK_ID, POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.empty());

            AvailableTargetsResponseDto result = careerTrackService.availableTargets(TRACK_ID, EMPLOYEE_ID);

            assertThat(result.getCurrentNodeId()).isNull();
            assertThat(result.getTargets()).isEmpty();
            verify(nodeRepository, never()).findAllByTrackIdOrderByDisplayOrderAsc(any());
        }

        @Test
        @DisplayName("returns no targets when the employee has no position")
        void shouldReturnEmptyWhenPositionMissing() {
            when(trackRepository.findById(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(null, GRADE_ID));

            AvailableTargetsResponseDto result = careerTrackService.availableTargets(TRACK_ID, EMPLOYEE_ID);

            assertThat(result.getCurrentNodeId()).isNull();
            assertThat(result.getTargets()).isEmpty();
            verifyNoInteractions(nodeRepository);
        }

        @Test
        @DisplayName("returns no targets when the employee has no grade")
        void shouldReturnEmptyWhenGradeMissing() {
            when(trackRepository.findById(TRACK_ID))
                    .thenReturn(Optional.of(track(CareerTrackStatus.PUBLISHED)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(POSITION_ID, null));

            AvailableTargetsResponseDto result = careerTrackService.availableTargets(TRACK_ID, EMPLOYEE_ID);

            assertThat(result.getTargets()).isEmpty();
            verifyNoInteractions(nodeRepository);
        }

        @ParameterizedTest
        @EnumSource(value = CareerTrackStatus.class, names = {"DRAFT", "ARCHIVED"})
        @DisplayName("hides an unpublished track after loading the employee")
        void shouldHideUnpublishedTrack(CareerTrackStatus status) {
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.of(track(status)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID))
                    .thenReturn(employee(POSITION_ID, GRADE_ID));

            assertThatThrownBy(() -> careerTrackService.availableTargets(TRACK_ID, EMPLOYEE_ID))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verify(employeeDirectory).requireEmployee(EMPLOYEE_ID);
            verifyNoInteractions(nodeRepository);
        }

        @Test
        @DisplayName("throws when the track does not exist")
        void shouldThrowWhenTrackMissing() {
            when(trackRepository.findById(TRACK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> careerTrackService.availableTargets(TRACK_ID, EMPLOYEE_ID))
                    .isInstanceOf(CareerTrackNotFoundException.class)
                    .hasMessageContaining(TRACK_ID.toString());

            verifyNoInteractions(employeeDirectory);
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("groups outgoing transitions, drops duplicate edges, and attaches the revision")
        void shouldAssembleGraph() {
            CareerTrack track = track(CareerTrackStatus.PUBLISHED);
            CareerTrackNode source = node(NODE_ID, TRACK_ID, 0);
            CareerTrackNode target = node(OTHER_NODE_ID, TRACK_ID, 1);
            CareerTransition first = transition(TRANSITION_ID, NODE_ID, OTHER_NODE_ID);
            CareerTransition second = transition(OTHER_TRANSITION_ID, OTHER_NODE_ID, NODE_ID);
            CareerTrackRevision revision = revision(REVISION_ID, 3, CareerRevisionStatus.PUBLISHED);
            CareerTrackRevisionResponseDto revisionDto = CareerTrackRevisionResponseDto.builder()
                    .id(REVISION_ID)
                    .revision(3)
                    .build();
            when(trackMapper.toResponseDto(track)).thenReturn(CareerTrackResponseDto.builder().id(TRACK_ID).build());
            when(nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(TRACK_ID)).thenReturn(List.of(source, target));
            when(transitionRepository.findAllBySourceNodeIdIn(any())).thenReturn(List.of(first, first, second));
            when(nodeMapper.toResponseDto(any(CareerTrackNode.class))).thenAnswer(invocation -> {
                CareerTrackNode node = invocation.getArgument(0);
                return CareerTrackNodeResponseDto.builder().id(node.getId()).build();
            });
            when(transitionMapper.toResponseDto(any(CareerTransition.class))).thenAnswer(invocation -> {
                CareerTransition transition = invocation.getArgument(0);
                return CareerTransitionResponseDto.builder()
                        .id(transition.getId())
                        .sourceNodeId(transition.getSourceNodeId())
                        .targetNodeId(transition.getTargetNodeId())
                        .build();
            });
            when(revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                    TRACK_ID,
                    CareerRevisionStatus.PUBLISHED
            )).thenReturn(Optional.of(revision));
            when(revisionMapper.toResponseDto(revision)).thenReturn(revisionDto);

            CareerTrackResponseDto result = careerTrackService.toResponse(track);

            assertThat(result.getNodes()).hasSize(2);
            assertThat(result.getNodes().get(0).getOutgoingTransitions())
                    .extracting(CareerTransitionResponseDto::getId)
                    .containsExactly(TRANSITION_ID);
            assertThat(result.getNodes().get(1).getOutgoingTransitions())
                    .extracting(CareerTransitionResponseDto::getId)
                    .containsExactly(OTHER_TRANSITION_ID);
            assertThat(result.getTransitions())
                    .extracting(CareerTransitionResponseDto::getId)
                    .containsExactly(TRANSITION_ID, OTHER_TRANSITION_ID);
            assertThat(result.getCurrentRevision()).isSameAs(revisionDto);
        }

        @Test
        @DisplayName("returns empty collections when the track has no nodes and no revision")
        void shouldReturnEmptyGraph() {
            CareerTrack track = track(CareerTrackStatus.DRAFT);
            CareerTrackResponseDto response = CareerTrackResponseDto.builder().id(TRACK_ID).build();
            when(trackMapper.toResponseDto(track)).thenReturn(response);
            when(nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(TRACK_ID)).thenReturn(List.of());
            when(revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                    TRACK_ID,
                    CareerRevisionStatus.PUBLISHED
            )).thenReturn(Optional.empty());

            CareerTrackResponseDto result = careerTrackService.toResponse(track);

            assertThat(result.getNodes()).isEmpty();
            assertThat(result.getTransitions()).isEmpty();
            assertThat(result.getCurrentRevision()).isNull();
            verify(transitionRepository, never()).findAllBySourceNodeIdIn(any());
            verifyNoInteractions(nodeMapper, transitionMapper, revisionMapper);
        }
    }

    private void assertPublishStoppedBeforeSnapshot() {
        verify(revisionRepository, never()).findTopByTrackIdOrderByRevisionDesc(any());
        verify(revisionRepository, never()).save(any());
        verifyNoInteractions(objectMapper, outboxEventService);
    }

    private void arrangePublish(
            CareerTrack track,
            List<CareerTrackNode> nodes,
            List<CareerTransition> transitions,
            Optional<CareerTrackRevision> latest,
            Optional<CareerTrackRevision> published
    ) {
        authenticate();
        arrangeStructure(track, nodes, transitions);
        when(revisionRepository.findTopByTrackIdOrderByRevisionDesc(TRACK_ID)).thenReturn(latest);
        when(revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                TRACK_ID,
                CareerRevisionStatus.PUBLISHED
        )).thenReturn(published);
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
        when(trackMapper.toResponseDto(any(CareerTrack.class))).thenAnswer(invocation -> {
            CareerTrack saved = invocation.getArgument(0);
            return CareerTrackResponseDto.builder()
                    .id(saved.getId())
                    .code(saved.getCode())
                    .name(saved.getName())
                    .status(saved.getStatus())
                    .build();
        });
        if (!nodes.isEmpty()) {
            when(nodeMapper.toResponseDto(any(CareerTrackNode.class))).thenAnswer(invocation -> {
                CareerTrackNode node = invocation.getArgument(0);
                return CareerTrackNodeResponseDto.builder().id(node.getId()).build();
            });
        }
        if (!transitions.isEmpty()) {
            when(transitionMapper.toResponseDto(any(CareerTransition.class))).thenAnswer(invocation -> {
                CareerTransition transition = invocation.getArgument(0);
                return CareerTransitionResponseDto.builder()
                        .id(transition.getId())
                        .sourceNodeId(transition.getSourceNodeId())
                        .targetNodeId(transition.getTargetNodeId())
                        .build();
            });
        }
    }

    private void arrangeStructure(
            CareerTrack track,
            List<CareerTrackNode> nodes,
            List<CareerTransition> transitions
    ) {
        when(trackRepository.findByIdForUpdate(TRACK_ID)).thenReturn(Optional.of(track));
        when(nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(TRACK_ID)).thenReturn(nodes);
        if (!nodes.isEmpty()) {
            when(transitionRepository.findAllBySourceNodeIdIn(any())).thenReturn(transitions);
        }
    }

    private void arrangeDraftTrack() {
        when(trackRepository.findByIdForUpdate(TRACK_ID))
                .thenReturn(Optional.of(track(CareerTrackStatus.DRAFT)));
    }

    private void arrangeNodeOnDraft(CareerTrackNode node) {
        when(nodeRepository.findById(node.getId())).thenReturn(Optional.of(node));
        arrangeDraftTrack();
    }

    private void arrangeDraftTransition(CareerTrackNode source, CareerTrackNode target) {
        arrangeDraftTrack();
        when(nodeRepository.findById(source.getId())).thenReturn(Optional.of(source));
        if (!source.getId().equals(target.getId())) {
            when(nodeRepository.findById(target.getId())).thenReturn(Optional.of(target));
        }
    }

    private CareerTrackResponseDto stubEmptyGraph() {
        CareerTrackResponseDto response = stubTrackMapperAndEmptyNodes();
        when(revisionRepository.findFirstByTrackIdAndStatusOrderByRevisionDesc(
                TRACK_ID,
                CareerRevisionStatus.PUBLISHED
        )).thenReturn(Optional.empty());
        return response;
    }

    private CareerTrackResponseDto stubTrackMapperAndEmptyNodes() {
        CareerTrackResponseDto response = CareerTrackResponseDto.builder().id(TRACK_ID).build();
        when(trackMapper.toResponseDto(any(CareerTrack.class))).thenAnswer(invocation -> {
            CareerTrack saved = invocation.getArgument(0);
            response.setId(saved.getId());
            response.setCode(saved.getCode());
            response.setName(saved.getName());
            response.setStatus(saved.getStatus());
            return response;
        });
        when(nodeRepository.findAllByTrackIdOrderByDisplayOrderAsc(TRACK_ID)).thenReturn(List.of());
        return response;
    }

    private void stubSave() {
        when(trackRepository.save(any(CareerTrack.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubSaveAndFlush() {
        when(trackRepository.saveAndFlush(any(CareerTrack.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubNodeSaveAndFlush() {
        when(nodeRepository.saveAndFlush(any(CareerTrackNode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(nodeMapper.toResponseDto(any(CareerTrackNode.class))).thenAnswer(invocation -> {
            CareerTrackNode node = invocation.getArgument(0);
            return CareerTrackNodeResponseDto.builder().id(node.getId()).label(node.getLabel()).build();
        });
    }

    private void applyTrackUpdate() {
        doAnswer(invocation -> {
            CareerTrackUpdateRequestDto request = invocation.getArgument(0);
            CareerTrack track = invocation.getArgument(1);
            if (request.getName() != null) {
                track.setName(request.getName());
            }
            if (request.getDescription() != null) {
                track.setDescription(request.getDescription());
            }
            if (request.getType() != null) {
                track.setType(request.getType());
            }
            return null;
        }).when(trackMapper).updateEntity(any(CareerTrackUpdateRequestDto.class), any(CareerTrack.class));
    }

    private void applyNodeUpdate() {
        doAnswer(invocation -> {
            CareerTrackNodeUpdateRequestDto request = invocation.getArgument(0);
            CareerTrackNode node = invocation.getArgument(1);
            if (request.getPositionId() != null) {
                node.setPositionId(request.getPositionId());
            }
            if (request.getGradeId() != null) {
                node.setGradeId(request.getGradeId());
            }
            if (request.getLabel() != null) {
                node.setLabel(request.getLabel());
            }
            if (request.getDescription() != null) {
                node.setDescription(request.getDescription());
            }
            if (request.getDisplayOrder() != null) {
                node.setDisplayOrder(request.getDisplayOrder());
            }
            if (request.getIsEntryPoint() != null) {
                node.setIsEntryPoint(request.getIsEntryPoint());
            }
            return null;
        }).when(nodeMapper).updateEntity(any(CareerTrackNodeUpdateRequestDto.class), any(CareerTrackNode.class));
    }

    @SuppressWarnings("unchecked")
    private Pageable assertListedWithStatus(CareerTrackStatus status) {
        ArgumentCaptor<Specification<CareerTrack>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(trackRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());

        Root<CareerTrack> root = mock(Root.class);
        CriteriaQuery<CareerTrack> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> statusPath = mock(Path.class);
        lenient().when(root.get(anyString())).thenReturn(statusPath);

        specificationCaptor.getValue().toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(statusPath, status);
        return pageableCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private void assertListedWithoutFieldPredicates() {
        ArgumentCaptor<Specification<CareerTrack>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(trackRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Root<CareerTrack> root = mock(Root.class);
        CriteriaQuery<CareerTrack> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);

        specificationCaptor.getValue().toPredicate(root, query, criteriaBuilder);

        verify(root, never()).get(anyString());
    }

    private CareerTrackNode captureSavedNode() {
        ArgumentCaptor<CareerTrackNode> captor = ArgumentCaptor.forClass(CareerTrackNode.class);
        verify(nodeRepository).save(captor.capture());
        return captor.getValue();
    }

    private CareerTransition captureSavedTransition() {
        ArgumentCaptor<CareerTransition> captor = ArgumentCaptor.forClass(CareerTransition.class);
        verify(transitionRepository).save(captor.capture());
        return captor.getValue();
    }

    private CareerTrack track(CareerTrackStatus status) {
        return CareerTrack.builder()
                .id(TRACK_ID)
                .code(CODE)
                .name(NAME)
                .description("Backend growth")
                .type(CareerTrackType.VERTICAL)
                .status(status)
                .version(VERSION)
                .createdBy(USER_ID)
                .build();
    }

    private CareerTrackNode node(UUID id, UUID trackId, int order) {
        return CareerTrackNode.builder()
                .id(id)
                .trackId(trackId)
                .positionId(POSITION_ID)
                .gradeId(GRADE_ID)
                .label("L" + order)
                .description("Node " + order)
                .displayOrder(order)
                .isEntryPoint(order == 0)
                .build();
    }

    private CareerTransition transition(UUID id, UUID sourceId, UUID targetId) {
        return CareerTransition.builder()
                .id(id)
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .transitionType("PROMOTION")
                .description("Move")
                .estimatedMonths(6)
                .active(true)
                .build();
    }

    private CareerTrackRevision revision(UUID id, int number, CareerRevisionStatus status) {
        return CareerTrackRevision.builder()
                .id(id)
                .trackId(TRACK_ID)
                .revision(number)
                .status(status)
                .snapshot("{}")
                .build();
    }

    private CareerTrackCreateRequestDto createRequest(String code) {
        return CareerTrackCreateRequestDto.builder()
                .code(code)
                .name(NAME)
                .description("Backend growth")
                .type(CareerTrackType.VERTICAL)
                .build();
    }

    private CareerTrackNodeCreateRequestDto nodeRequest(Boolean entryPoint) {
        return CareerTrackNodeCreateRequestDto.builder()
                .positionId(POSITION_ID)
                .gradeId(GRADE_ID)
                .label("Senior")
                .description("Senior node")
                .displayOrder(3)
                .isEntryPoint(entryPoint)
                .build();
    }

    private CareerTransitionCreateRequestDto transitionRequest(UUID sourceId, UUID targetId, Boolean active) {
        return CareerTransitionCreateRequestDto.builder()
                .sourceNodeId(sourceId)
                .targetNodeId(targetId)
                .transitionType("PROMOTION")
                .description("Promote")
                .estimatedMonths(12)
                .active(active)
                .build();
    }

    private CareerTrackPublicationRequestDto publication(Long expectedVersion) {
        return CareerTrackPublicationRequestDto.builder()
                .expectedVersion(expectedVersion)
                .build();
    }

    private PageRequestDto pageRequest() {
        return PageRequestDto.builder()
                .page(1)
                .size(5)
                .sortBy("code")
                .sortDirection(SortDirection.ASC)
                .build();
    }

    private EmployeeLookupItemDto employee(UUID positionId, UUID gradeId) {
        return new EmployeeLookupItemDto(EMPLOYEE_ID, "Ada Lovelace", true, positionId, gradeId);
    }

    private void denyHr() {
        doThrow(new AccessDeniedException("Недостаточно прав для управления карьерными справочниками"))
                .when(accessService).requireHrOrAdmin();
    }

    private void denyEmployee(UUID employeeId) {
        doThrow(new AccessDeniedException("Нет доступа к данным выбранного сотрудника"))
                .when(accessService).requireSelfOrManager(employeeId);
    }

    private void authenticate() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId(USER_ID)
                .role(SecurityConstants.HR_AUTHORITY)
                .email("hr@company.com")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        List.of(new SimpleGrantedAuthority(SecurityConstants.HR_AUTHORITY))
                )
        );
    }
}
