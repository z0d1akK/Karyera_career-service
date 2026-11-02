package com.zodiakk.careerservice.talent.service;

import com.zodiakk.careerservice.career.dto.response.PositionRequirementResponseDto;
import com.zodiakk.careerservice.career.service.PositionRequirementService;
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import com.zodiakk.careerservice.common.enums.ReserveCandidateStatus;
import com.zodiakk.careerservice.common.enums.ReserveDecisionType;
import com.zodiakk.careerservice.common.enums.TalentReserveStatus;
import com.zodiakk.careerservice.common.exception.domain.DuplicateResourceException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.common.exception.domain.TalentReserveNotFoundException;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningResultDto;
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
import com.zodiakk.careerservice.talent.dto.request.update.TalentReserveUpdateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveCandidateResponseDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveDecisionResponseDto;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TalentReserveServiceTest {

    private static final UUID RESERVE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID EMPLOYEE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID MISSING_EMPLOYEE_ID = UUID.fromString("34343434-3434-3434-3434-343434343434");

    private static final UUID UNASSIGNED_EMPLOYEE_ID = UUID.fromString("35353535-3535-3535-3535-353535353535");

    private static final UUID UNKNOWN_REF_EMPLOYEE_ID = UUID.fromString("36363636-3636-3636-3636-363636363636");

    private static final UUID NAMED_EMPLOYEE_ID = UUID.fromString("37373737-3737-3737-3737-373737373737");

    private static final UUID ACTOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID POSITION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final UUID GRADE_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID UNKNOWN_POSITION_ID = UUID.fromString("68686868-6868-6868-6868-686868686868");

    private static final UUID UNKNOWN_GRADE_ID = UUID.fromString("78787878-7878-7878-7878-787878787878");

    private static final UUID REQUIREMENT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final UUID JOB_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID ASSESSMENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID DECISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final Long VERSION = 4L;

    private static final List<ReserveCandidateStatus> ACTIVE_STATUSES = List.of(
            ReserveCandidateStatus.NOMINATED,
            ReserveCandidateStatus.UNDER_REVIEW,
            ReserveCandidateStatus.APPROVED
    );

    @Mock
    private TalentReserveRepository reserveRepository;

    @Mock
    private ReserveCandidateRepository candidateRepository;

    @Mock
    private ReserveDecisionRepository decisionRepository;

    @Mock
    private TalentReserveMapper reserveMapper;

    @Mock
    private ReserveCandidateMapper candidateMapper;

    @Mock
    private ReserveDecisionMapper decisionMapper;

    @Mock
    private EmployeeDirectoryService employeeDirectory;

    @Mock
    private PositionRequirementService requirementService;

    @Mock
    private AnalyticsIntegrationService analyticsIntegrationService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private CareerAccessService accessService;

    @InjectMocks
    private TalentReserveService service;

    @Nested
    @DisplayName("list")
    class ListReserves {

        @Test
        @DisplayName("denies access before reading reserves")
        void shouldDenyAccess() {
            doThrow(new AccessDeniedException("Недостаточно прав для просмотра данных сотрудника"))
                    .when(accessService).requireManagerOrAbove();

            assertThatThrownBy(() -> service.list(null, page()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(reserveRepository);
        }

        @Test
        @DisplayName("maps a page and attaches the candidate count")
        void shouldListReserves() {
            TalentReserve reserve = reserve(TalentReserveStatus.DRAFT);
            when(reserveRepository.findAll(anyReserveSpecification(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(reserve)));
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID))
                    .thenReturn(List.of(candidate(ReserveCandidateStatus.NOMINATED)));

            assertThat(service.list(null, page()).getContent())
                    .singleElement()
                    .satisfies(response -> {
                        assertThat(response.getStatus()).isEqualTo(TalentReserveStatus.DRAFT);
                        assertThat(response.getCandidateCount()).isEqualTo(1L);
                    });
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates a draft reserve with a trimmed name and the current user")
        void shouldCreateDraft() {
            when(employeeDirectory.requirePosition(POSITION_ID))
                    .thenReturn(new ReferenceLookupItemDto(POSITION_ID, "Engineer", true));
            when(employeeDirectory.requireGrade(GRADE_ID))
                    .thenReturn(new ReferenceLookupItemDto(GRADE_ID, "Middle", true));
            when(accessService.currentUserId()).thenReturn(USER_ID);
            when(reserveMapper.toEntity(any(TalentReserveCreateRequestDto.class))).thenAnswer(invocation -> {
                TalentReserveCreateRequestDto dto = invocation.getArgument(0);
                return TalentReserve.builder()
                        .name("mapper")
                        .status(TalentReserveStatus.ACTIVE)
                        .description(dto.getDescription())
                        .targetPositionId(dto.getTargetPositionId())
                        .targetGradeId(dto.getTargetGradeId())
                        .effectiveFrom(dto.getEffectiveFrom())
                        .effectiveTo(dto.getEffectiveTo())
                        .build();
            });
            when(reserveRepository.saveAndFlush(any(TalentReserve.class))).thenAnswer(invocation -> {
                TalentReserve reserve = invocation.getArgument(0);
                reserve.setId(RESERVE_ID);
                return reserve;
            });
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());

            TalentReserveCreateRequestDto dto = TalentReserveCreateRequestDto.builder()
                    .name("  Leadership  ")
                    .description("Pool")
                    .targetPositionId(POSITION_ID)
                    .targetGradeId(GRADE_ID)
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .effectiveTo(LocalDate.of(2026, 1, 1))
                    .build();

            TalentReserveResponseDto response = service.create(dto);

            assertThat(response.getName()).isEqualTo("Leadership");
            assertThat(response.getStatus()).isEqualTo(TalentReserveStatus.DRAFT);
            assertThat(response.getCreatedBy()).isEqualTo(USER_ID);
            assertThat(response.getCandidateCount()).isZero();
        }

        @Test
        @DisplayName("rejects an effective period that ends before it starts")
        void shouldRejectInvertedPeriod() {
            when(employeeDirectory.requirePosition(POSITION_ID))
                    .thenReturn(new ReferenceLookupItemDto(POSITION_ID, "Engineer", true));
            when(employeeDirectory.requireGrade(GRADE_ID))
                    .thenReturn(new ReferenceLookupItemDto(GRADE_ID, "Middle", true));

            TalentReserveCreateRequestDto dto = TalentReserveCreateRequestDto.builder()
                    .name("Leadership")
                    .targetPositionId(POSITION_ID)
                    .targetGradeId(GRADE_ID)
                    .effectiveFrom(LocalDate.of(2026, 6, 1))
                    .effectiveTo(LocalDate.of(2026, 1, 1))
                    .build();

            assertThatThrownBy(() -> service.create(dto))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Дата окончания действия кадрового резерва не может быть раньше даты начала");

            verify(reserveMapper, never()).toEntity(any());
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("throws when the reserve does not exist")
        void shouldThrowWhenMissing() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(RESERVE_ID))
                    .isInstanceOf(TalentReserveNotFoundException.class)
                    .hasMessage("Кадровый резерв с идентификатором " + RESERVE_ID + " не найден");
        }

        @Test
        @DisplayName("returns the reserve after a manager check")
        void shouldReturnReserve() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());

            assertThat(service.get(RESERVE_ID).getId()).isEqualTo(RESERVE_ID);
            verify(accessService).requireManagerOrAbove();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates an open reserve, trims the name and keeps the workflow status")
        void shouldUpdateOpenReserve() {
            TalentReserve reserve = reserve(TalentReserveStatus.ACTIVE);
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve));
            when(employeeDirectory.requirePosition(POSITION_ID))
                    .thenReturn(new ReferenceLookupItemDto(POSITION_ID, "Engineer", true));
            when(employeeDirectory.requireGrade(GRADE_ID))
                    .thenReturn(new ReferenceLookupItemDto(GRADE_ID, "Middle", true));
            doAnswer(invocation -> {
                TalentReserve entity = invocation.getArgument(1);
                entity.setStatus(TalentReserveStatus.ARCHIVED);
                entity.setName("mapper");
                return null;
            }).when(reserveMapper).updateEntity(any(), eq(reserve));
            when(reserveRepository.saveAndFlush(reserve)).thenReturn(reserve);
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());

            TalentReserveResponseDto response = service.update(RESERVE_ID, TalentReserveUpdateRequestDto.builder()
                    .name("  Renamed  ")
                    .targetPositionId(POSITION_ID)
                    .targetGradeId(GRADE_ID)
                    .build());

            assertThat(reserve.getStatus()).isEqualTo(TalentReserveStatus.ACTIVE);
            assertThat(reserve.getName()).isEqualTo("Renamed");
            assertThat(response.getName()).isEqualTo("Renamed");
        }

        @Test
        @DisplayName("leaves the stored name unchanged when the request omits it")
        void shouldKeepNameWhenOmitted() {
            TalentReserve reserve = reserve(TalentReserveStatus.DRAFT);
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve));
            doAnswer(invocation -> null).when(reserveMapper).updateEntity(any(), eq(reserve));
            when(reserveRepository.saveAndFlush(reserve)).thenReturn(reserve);
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());

            service.update(RESERVE_ID, TalentReserveUpdateRequestDto.builder().description("notes").build());

            assertThat(reserve.getName()).isEqualTo("Leadership");
            verify(employeeDirectory, never()).requirePosition(any());
            verify(employeeDirectory, never()).requireGrade(any());
        }

        @Test
        @DisplayName("rejects an effective period that becomes inverted")
        void shouldRejectInvertedPeriod() {
            TalentReserve reserve = reserve(TalentReserveStatus.DRAFT);
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve));
            doAnswer(invocation -> {
                TalentReserve entity = invocation.getArgument(1);
                entity.setEffectiveFrom(LocalDate.of(2026, 8, 1));
                entity.setEffectiveTo(LocalDate.of(2026, 1, 1));
                return null;
            }).when(reserveMapper).updateEntity(any(), eq(reserve));

            assertThatThrownBy(() -> service.update(RESERVE_ID, new TalentReserveUpdateRequestDto()))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Дата окончания действия кадрового резерва не может быть раньше даты начала");

            verify(reserveRepository, never()).saveAndFlush(any());
        }

        @ParameterizedTest
        @EnumSource(value = TalentReserveStatus.class, names = {"CLOSED", "ARCHIVED"})
        @DisplayName("rejects changes to a closed or archived reserve")
        void shouldRejectClosedReserve(TalentReserveStatus status) {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));

            assertThatThrownBy(() -> service.update(RESERVE_ID, TalentReserveUpdateRequestDto.builder()
                    .name("Renamed")
                    .build()))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Кадровый резерв в состоянии %s не может быть переведён в состояние изменение"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("activates a draft, including when the transition request is omitted")
        void shouldActivateDraft() {
            TalentReserve reserve = reserve(TalentReserveStatus.DRAFT);
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve));
            when(reserveRepository.saveAndFlush(reserve)).thenReturn(reserve);
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());

            assertThat(service.activate(RESERVE_ID, null).getStatus()).isEqualTo(TalentReserveStatus.ACTIVE);
        }

        @ParameterizedTest
        @EnumSource(value = TalentReserveStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
        @DisplayName("rejects activation from any status other than draft")
        void shouldRejectActivation(TalentReserveStatus status) {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));

            assertThatThrownBy(() -> service.activate(RESERVE_ID, transition(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Кадровый резерв в состоянии %s не может быть переведён в состояние ACTIVE"
                            .formatted(status.name()));
        }

        @Test
        @DisplayName("rejects a stale expected version")
        void shouldRejectVersionMismatch() {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID))
                    .thenReturn(Optional.of(reserve(TalentReserveStatus.DRAFT)));

            assertThatThrownBy(() -> service.activate(RESERVE_ID, transition(99L)))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessage("Ожидаемая версия не совпадает с текущей");
        }
    }

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("closes an active reserve")
        void shouldCloseActiveReserve() {
            TalentReserve reserve = reserve(TalentReserveStatus.ACTIVE);
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve));
            when(reserveRepository.saveAndFlush(reserve)).thenReturn(reserve);
            stubReserveMapper();
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());

            assertThat(service.close(RESERVE_ID, transition(null)).getStatus())
                    .isEqualTo(TalentReserveStatus.CLOSED);
        }

        @ParameterizedTest
        @EnumSource(value = TalentReserveStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
        @DisplayName("rejects closing from any status other than active")
        void shouldRejectClose(TalentReserveStatus status) {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));

            assertThatThrownBy(() -> service.close(RESERVE_ID, transition(VERSION)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Кадровый резерв в состоянии %s не может быть переведён в состояние CLOSED"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("candidates")
    class Candidates {

        @Test
        @DisplayName("scopes a null filter to the reserve")
        void shouldListWithDefaultFilter() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(candidateRepository.findAll(anyCandidateSpecification(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThatCode(() -> service.candidates(RESERVE_ID, null, page())).doesNotThrowAnyException();
            verify(accessService).requireManagerOrAbove();
        }

        @Test
        @DisplayName("forces the reserve id onto the supplied filter and maps decisions")
        void shouldBindReserveFilter() {
            ReserveCandidateFilterDto filter = ReserveCandidateFilterDto.builder()
                    .employeeId(EMPLOYEE_ID)
                    .build();
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.NOMINATED);
            ReserveDecision decision = ReserveDecision.builder()
                    .id(DECISION_ID)
                    .candidateId(CANDIDATE_ID)
                    .decision(ReserveDecisionType.APPROVE)
                    .build();

            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(candidateRepository.findAll(anyCandidateSpecification(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(candidate)));
            when(candidateMapper.toResponseDto(candidate)).thenReturn(ReserveCandidateResponseDto.builder()
                    .id(CANDIDATE_ID)
                    .build());
            when(decisionRepository.findAllByCandidateIdOrderByDecidedAtDesc(CANDIDATE_ID))
                    .thenReturn(List.of(decision));
            when(decisionMapper.toResponseDto(decision))
                    .thenReturn(ReserveDecisionResponseDto.builder().id(DECISION_ID).build());

            var page = service.candidates(RESERVE_ID, filter, page());

            assertThat(filter.getReserveId()).isEqualTo(RESERVE_ID);
            assertThat(page.getContent()).singleElement().satisfies(response ->
                    assertThat(response.getDecisions()).extracting(ReserveDecisionResponseDto::getId)
                            .containsExactly(DECISION_ID));
        }
    }

    @Nested
    @DisplayName("nominate")
    class Nominate {

        @ParameterizedTest
        @EnumSource(value = TalentReserveStatus.class, names = {"DRAFT", "ACTIVE"})
        @DisplayName("nominates an active employee into a draft or active reserve")
        void shouldNominate(TalentReserveStatus status) {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, true, null, null));
            when(candidateRepository.existsByReserveIdAndEmployeeIdAndStatusIn(
                    RESERVE_ID,
                    EMPLOYEE_ID,
                    ACTIVE_STATUSES
            )).thenReturn(false);
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(candidateRepository.save(any(ReserveCandidate.class))).thenAnswer(invocation -> {
                ReserveCandidate candidate = invocation.getArgument(0);
                candidate.setId(CANDIDATE_ID);
                return candidate;
            });
            when(candidateMapper.toResponseDto(any(ReserveCandidate.class))).thenAnswer(invocation -> {
                ReserveCandidate candidate = invocation.getArgument(0);
                return ReserveCandidateResponseDto.builder()
                        .id(candidate.getId())
                        .status(candidate.getStatus())
                        .nominatedBy(candidate.getNominatedBy())
                        .assessmentId(candidate.getAssessmentId())
                        .build();
            });
            when(decisionRepository.findAllByCandidateIdOrderByDecidedAtDesc(CANDIDATE_ID)).thenReturn(List.of());

            ReserveCandidateResponseDto response = service.nominate(RESERVE_ID, nomination());

            assertThat(response.getStatus()).isEqualTo(ReserveCandidateStatus.NOMINATED);
            assertThat(response.getNominatedBy()).isEqualTo(ACTOR_ID);
            assertThat(response.getAssessmentId()).isEqualTo(ASSESSMENT_ID);
            ArgumentCaptor<ReserveCandidate> captor = ArgumentCaptor.forClass(ReserveCandidate.class);
            verify(candidateRepository).save(captor.capture());
            assertThat(captor.getValue().getNominatedAt()).isNotNull();
            assertThat(captor.getValue().getNominatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        }

        @Test
        @DisplayName("rejects an inactive employee")
        void shouldRejectInactiveEmployee() {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID))
                    .thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, false, null, null));

            assertThatThrownBy(() -> service.nominate(RESERVE_ID, nomination()))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Неактивного сотрудника нельзя включить в кадровый резерв");
        }

        @Test
        @DisplayName("rejects an employee who is already an active candidate")
        void shouldRejectDuplicate() {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID))
                    .thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(employeeDirectory.requireEmployee(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, true, null, null));
            when(candidateRepository.existsByReserveIdAndEmployeeIdAndStatusIn(
                    RESERVE_ID,
                    EMPLOYEE_ID,
                    ACTIVE_STATUSES
            )).thenReturn(true);

            assertThatThrownBy(() -> service.nominate(RESERVE_ID, nomination()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Сотрудник уже включён в кадровый резерв");
        }

        @ParameterizedTest
        @EnumSource(value = TalentReserveStatus.class, names = {"CLOSED", "ARCHIVED"})
        @DisplayName("rejects nomination into a closed or archived reserve")
        void shouldRejectClosedReserve(TalentReserveStatus status) {
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));

            assertThatThrownBy(() -> service.nominate(RESERVE_ID, nomination()))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Номинация в кадровый резерв в состоянии %s не может быть переведён в состояние номинация"
                            .formatted(status.name()));
        }
    }

    @Nested
    @DisplayName("getCandidate")
    class GetCandidate {

        @Test
        @DisplayName("throws when the candidate does not exist")
        void shouldThrowWhenMissing() {
            when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCandidate(CANDIDATE_ID))
                    .isInstanceOf(TalentReserveNotFoundException.class)
                    .hasMessage("Кандидат кадрового резерва с идентификатором " + CANDIDATE_ID + " не найден");
        }
    }

    @Nested
    @DisplayName("decide")
    class Decide {

        @ParameterizedTest(name = "{0} + {1} allowed={2}")
        @CsvSource({
                "NOMINATED, APPROVE, true",
                "UNDER_REVIEW, APPROVE, true",
                "APPROVED, APPROVE, false",
                "REJECTED, APPROVE, false",
                "REMOVED, APPROVE, false",
                "NOMINATED, REJECT, true",
                "UNDER_REVIEW, REJECT, true",
                "APPROVED, REJECT, false",
                "REJECTED, REJECT, false",
                "REMOVED, REJECT, false",
                "NOMINATED, REQUEST_REVIEW, true",
                "UNDER_REVIEW, REQUEST_REVIEW, true",
                "APPROVED, REQUEST_REVIEW, false",
                "REJECTED, REQUEST_REVIEW, false",
                "REMOVED, REQUEST_REVIEW, false",
                "NOMINATED, REMOVE, true",
                "UNDER_REVIEW, REMOVE, true",
                "APPROVED, REMOVE, true",
                "REJECTED, REMOVE, false",
                "REMOVED, REMOVE, false"
        })
        @DisplayName("applies a decision only from the statuses allowed for that decision")
        void shouldApplyDecisionMatrix(
                ReserveCandidateStatus status,
                ReserveDecisionType decision,
                boolean allowed
        ) {
            ReserveCandidate candidate = candidate(status);
            when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
            when(reserveRepository.findByIdForUpdate(RESERVE_ID))
                    .thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(candidateRepository.findByIdForUpdate(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
            lenient().when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            lenient().when(decisionRepository.save(any(ReserveDecision.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(decisionMapper.toResponseDto(any(ReserveDecision.class))).thenAnswer(invocation -> {
                ReserveDecision saved = invocation.getArgument(0);
                return ReserveDecisionResponseDto.builder().decision(saved.getDecision()).build();
            });

            ReserveCandidateDecisionCreateRequestDto dto = decision(decision, null);

            if (allowed) {
                assertThat(service.decide(CANDIDATE_ID, dto).getDecision()).isEqualTo(decision);
                assertThat(candidate.getStatus()).isEqualTo(expectedStatus(decision));
                if (decision == ReserveDecisionType.APPROVE) {
                    verify(outboxEventService).saveEvent(any(), any(), any(), anyInt(), any());
                } else {
                    verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
                }
            } else {
                assertThatThrownBy(() -> service.decide(CANDIDATE_ID, dto))
                        .isInstanceOf(InvalidStateTransitionException.class)
                        .hasMessage("Кандидат кадрового резерва в состоянии %s не может быть переведён в состояние %s"
                                .formatted(status.name(), decision.name()));
                verify(decisionRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("approves a nominated candidate, clears removal and publishes the event")
        void shouldApproveCandidate() {
            OffsetDateTime removedAt = OffsetDateTime.parse("2026-02-01T00:00:00Z");
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.NOMINATED);
            candidate.setAssessmentId(ASSESSMENT_ID);
            candidate.setRemovedAt(removedAt);
            TalentReserve reserve = reserve(TalentReserveStatus.DRAFT);
            when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve));
            when(candidateRepository.findByIdForUpdate(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
            when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
            when(decisionRepository.save(any(ReserveDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(decisionMapper.toResponseDto(any(ReserveDecision.class))).thenAnswer(invocation -> {
                ReserveDecision saved = invocation.getArgument(0);
                return ReserveDecisionResponseDto.builder()
                        .decision(saved.getDecision())
                        .assessmentId(saved.getAssessmentId())
                        .decidedByEmployeeId(saved.getDecidedByEmployeeId())
                        .comment(saved.getComment())
                        .build();
            });

            ReserveDecisionResponseDto response = service.decide(
                    CANDIDATE_ID,
                    decision(ReserveDecisionType.APPROVE, ASSESSMENT_ID)
            );

            assertThat(candidate.getStatus()).isEqualTo(ReserveCandidateStatus.APPROVED);
            assertThat(candidate.getIncludedAt()).isNotNull();
            assertThat(candidate.getIncludedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(candidate.getRemovedAt()).isNull();
            assertThat(candidate.getAssessmentId()).isEqualTo(ASSESSMENT_ID);
            assertThat(response.getDecision()).isEqualTo(ReserveDecisionType.APPROVE);
            assertThat(response.getDecidedByEmployeeId()).isEqualTo(ACTOR_ID);
            assertThat(response.getComment()).isEqualTo("note");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
            verify(outboxEventService).saveEvent(
                    eq(CANDIDATE_ID),
                    eq(KafkaEventConstants.AGGREGATE_TYPE_RESERVE_CANDIDATE),
                    eq(KafkaEventConstants.TALENT_RESERVE_CANDIDATE_APPROVED),
                    eq(KafkaEventConstants.EVENT_VERSION_V1),
                    payload.capture()
            );
            assertThat(payload.getValue())
                    .containsEntry("candidateId", CANDIDATE_ID)
                    .containsEntry("reserveId", RESERVE_ID)
                    .containsEntry("employeeId", EMPLOYEE_ID)
                    .containsEntry("targetPositionId", POSITION_ID)
                    .containsEntry("targetGradeId", GRADE_ID)
                    .containsEntry("approvedAt", candidate.getIncludedAt());
        }

        @Test
        @DisplayName("keeps the original inclusion timestamp when the candidate is already included")
        void shouldKeepExistingInclusionTimestamp() {
            OffsetDateTime includedAt = OffsetDateTime.parse("2026-01-15T00:00:00Z");
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.UNDER_REVIEW);
            candidate.setIncludedAt(includedAt);
            arrangeDecision(candidate, TalentReserveStatus.ACTIVE);

            service.decide(CANDIDATE_ID, decision(ReserveDecisionType.APPROVE, null));

            assertThat(candidate.getIncludedAt()).isEqualTo(includedAt);
            assertThat(candidate.getAssessmentId()).isNull();
        }

        @Test
        @DisplayName("rejects a candidate and records the removal time")
        void shouldRejectCandidate() {
            OffsetDateTime includedAt = OffsetDateTime.parse("2026-01-15T00:00:00Z");
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.UNDER_REVIEW);
            candidate.setIncludedAt(includedAt);
            arrangeDecision(candidate, TalentReserveStatus.ACTIVE);

            service.decide(CANDIDATE_ID, decision(ReserveDecisionType.REJECT, null));

            assertThat(candidate.getStatus()).isEqualTo(ReserveCandidateStatus.REJECTED);
            assertThat(candidate.getIncludedAt()).isEqualTo(includedAt);
            assertThat(candidate.getRemovedAt()).isNotNull();
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("moves a candidate back to review without inclusion or removal timestamps")
        void shouldRequestReview() {
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.NOMINATED);
            arrangeDecision(candidate, TalentReserveStatus.ACTIVE);

            service.decide(CANDIDATE_ID, decision(ReserveDecisionType.REQUEST_REVIEW, null));

            assertThat(candidate.getStatus()).isEqualTo(ReserveCandidateStatus.UNDER_REVIEW);
            assertThat(candidate.getIncludedAt()).isNull();
            assertThat(candidate.getRemovedAt()).isNull();
        }

        @Test
        @DisplayName("removes an approved candidate without publishing an approval event")
        void shouldRemoveApprovedCandidate() {
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.APPROVED);
            arrangeDecision(candidate, TalentReserveStatus.ACTIVE);

            service.decide(CANDIDATE_ID, decision(ReserveDecisionType.REMOVE, null));

            assertThat(candidate.getStatus()).isEqualTo(ReserveCandidateStatus.REMOVED);
            assertThat(candidate.getRemovedAt()).isNotNull();
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @ParameterizedTest
        @EnumSource(value = TalentReserveStatus.class, names = {"CLOSED", "ARCHIVED"})
        @DisplayName("rejects a decision when the reserve is closed or archived")
        void shouldRejectClosedReserve(TalentReserveStatus status) {
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.NOMINATED);
            when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
            when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));
            when(candidateRepository.findByIdForUpdate(CANDIDATE_ID)).thenReturn(Optional.of(candidate));

            assertThatThrownBy(() -> service.decide(CANDIDATE_ID, decision(ReserveDecisionType.APPROVE, null)))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Кадровый резерв в состоянии %s не может быть переведён в состояние принятие решения по кандидату"
                            .formatted(status.name()));

            verify(decisionRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a stale candidate version")
        void shouldRejectVersionMismatch() {
            ReserveCandidate candidate = candidate(ReserveCandidateStatus.NOMINATED);
            when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
            when(reserveRepository.findByIdForUpdate(RESERVE_ID))
                    .thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(candidateRepository.findByIdForUpdate(CANDIDATE_ID)).thenReturn(Optional.of(candidate));

            ReserveCandidateDecisionCreateRequestDto dto = decision(ReserveDecisionType.APPROVE, null);
            dto.setExpectedVersion(99L);

            assertThatThrownBy(() -> service.decide(CANDIDATE_ID, dto))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessage("Ожидаемая версия не совпадает с текущей");

            verify(decisionRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates an access denial before loading the candidate")
        void shouldDenyAccess() {
            doThrow(new AccessDeniedException("Недостаточно прав для управления карьерными справочниками"))
                    .when(accessService).requireHrOrAdmin();

            assertThatThrownBy(() -> service.decide(CANDIDATE_ID, decision(ReserveDecisionType.APPROVE, null)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(candidateRepository, reserveRepository);
        }
    }

    @Nested
    @DisplayName("decisions")
    class Decisions {

        @Test
        @DisplayName("returns the candidate decision history")
        void shouldListDecisions() {
            ReserveDecision decision = ReserveDecision.builder()
                    .id(DECISION_ID)
                    .candidateId(CANDIDATE_ID)
                    .decision(ReserveDecisionType.APPROVE)
                    .build();
            when(candidateRepository.findById(CANDIDATE_ID))
                    .thenReturn(Optional.of(candidate(ReserveCandidateStatus.APPROVED)));
            when(decisionRepository.findAll(anyDecisionSpecification(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(decision)));
            when(decisionMapper.toResponseDto(decision))
                    .thenReturn(ReserveDecisionResponseDto.builder().id(DECISION_ID).build());

            assertThat(service.decisions(CANDIDATE_ID, page()).getContent())
                    .extracting(ReserveDecisionResponseDto::getId)
                    .containsExactly(DECISION_ID);
        }

        @Test
        @DisplayName("throws when the candidate does not exist")
        void shouldThrowWhenCandidateMissing() {
            when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.decisions(CANDIDATE_ID, page()))
                    .isInstanceOf(TalentReserveNotFoundException.class);

            verifyNoInteractions(decisionRepository);
        }
    }

    @Nested
    @DisplayName("startScreening")
    class StartScreening {

        @Test
        @DisplayName("starts screening against the current requirement of an active reserve")
        void shouldStartScreening() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(requirementService.current(POSITION_ID, GRADE_ID))
                    .thenReturn(PositionRequirementResponseDto.builder().id(REQUIREMENT_ID).build());
            when(analyticsIntegrationService.startScreening(any(ScreeningStartRequestDto.class)))
                    .thenReturn(new ScreeningStartResponseDto(JOB_ID, "RUNNING"));

            TalentScreeningStartResponseDto response = service.startScreening(RESERVE_ID);

            assertThat(response.getReserveId()).isEqualTo(RESERVE_ID);
            assertThat(response.getJobId()).isEqualTo(JOB_ID);
            assertThat(response.getStatus()).isEqualTo("RUNNING");

            ArgumentCaptor<ScreeningStartRequestDto> captor =
                    ArgumentCaptor.forClass(ScreeningStartRequestDto.class);
            verify(analyticsIntegrationService).startScreening(captor.capture());
            assertThat(captor.getValue().reserveId()).isEqualTo(RESERVE_ID);
            assertThat(captor.getValue().targetPositionId()).isEqualTo(POSITION_ID);
            assertThat(captor.getValue().targetGradeId()).isEqualTo(GRADE_ID);
            assertThat(captor.getValue().requirementId()).isEqualTo(REQUIREMENT_ID);
        }

        @Test
        @DisplayName("returns empty job fields when analytics returns no body")
        void shouldTolerateNullScreeningResponse() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(requirementService.current(POSITION_ID, GRADE_ID))
                    .thenReturn(PositionRequirementResponseDto.builder().id(REQUIREMENT_ID).build());
            when(analyticsIntegrationService.startScreening(any())).thenReturn(null);

            TalentScreeningStartResponseDto response = service.startScreening(RESERVE_ID);

            assertThat(response.getReserveId()).isEqualTo(RESERVE_ID);
            assertThat(response.getJobId()).isNull();
            assertThat(response.getStatus()).isNull();
        }

        @Test
        @DisplayName("rejects screening unless the reserve is active")
        void shouldRejectInactiveReserve() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.DRAFT)));

            assertThatThrownBy(() -> service.startScreening(RESERVE_ID))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Кадровый резерв в состоянии DRAFT не может быть переведён в состояние предварительный отбор");

            verifyNoInteractions(requirementService, analyticsIntegrationService);
        }
    }

    @Nested
    @DisplayName("getScreening")
    class GetScreening {

        @Test
        @DisplayName("copies the analytics screening onto the reserve response")
        void shouldReturnScreening() {
            ScreeningResultDto result = new ScreeningResultDto(
                    JOB_ID,
                    null,
                    JOB_ID,
                    EMPLOYEE_ID,
                    ASSESSMENT_ID,
                    1,
                    BigDecimal.ONE,
                    "ELIGIBLE",
                    true,
                    null
            );
            OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-01T00:00:00Z");
            OffsetDateTime completedAt = OffsetDateTime.parse("2026-03-02T00:00:00Z");
            TalentScreeningDto screening = new TalentScreeningDto(
                    JOB_ID,
                    createdAt,
                    JOB_ID,
                    RESERVE_ID,
                    POSITION_ID,
                    GRADE_ID,
                    REQUIREMENT_ID,
                    2,
                    "v1",
                    "COMPLETED",
                    4,
                    3,
                    1,
                    completedAt,
                    List.of(result)
            );
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(analyticsIntegrationService.getScreening(JOB_ID)).thenReturn(screening);

            TalentScreeningResponseDto response = service.getScreening(RESERVE_ID, JOB_ID);

            assertThat(response.getReserveId()).isEqualTo(RESERVE_ID);
            assertThat(response.getJobId()).isEqualTo(JOB_ID);
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getTargetPositionId()).isEqualTo(POSITION_ID);
            assertThat(response.getTargetGradeId()).isEqualTo(GRADE_ID);
            assertThat(response.getRequirementId()).isEqualTo(REQUIREMENT_ID);
            assertThat(response.getRequirementRevision()).isEqualTo(2);
            assertThat(response.getMethodologyVersion()).isEqualTo("v1");
            assertThat(response.getCandidateCount()).isEqualTo(4);
            assertThat(response.getEvaluatedCount()).isEqualTo(3);
            assertThat(response.getFailedCount()).isEqualTo(1);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
            assertThat(response.getCompletedAt()).isEqualTo(completedAt);
            assertThat(response.getResults()).containsExactly(result);
        }

        @Test
        @DisplayName("returns the requested job id and an empty result list when analytics returns no body")
        void shouldTolerateNullScreening() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(analyticsIntegrationService.getScreening(JOB_ID)).thenReturn(null);

            TalentScreeningResponseDto response = service.getScreening(RESERVE_ID, JOB_ID);

            assertThat(response.getJobId()).isEqualTo(JOB_ID);
            assertThat(response.getStatus()).isNull();
            assertThat(response.getResults()).isEmpty();
            assertThat(response.getCandidateCount()).isNull();
        }
    }

    @Nested
    @DisplayName("analysis")
    class Analysis {

        @Test
        @DisplayName("groups candidates by status, position and grade, skipping unknown employees")
        void shouldAnalyzeCandidates() {
            ReserveCandidate rejected = candidate(MISSING_EMPLOYEE_ID, ReserveCandidateStatus.REJECTED);
            ReserveCandidate nominated = candidate(UNASSIGNED_EMPLOYEE_ID, ReserveCandidateStatus.NOMINATED);
            ReserveCandidate approved = candidate(UNKNOWN_REF_EMPLOYEE_ID, ReserveCandidateStatus.APPROVED);
            ReserveCandidate reviewing = candidate(NAMED_EMPLOYEE_ID, ReserveCandidateStatus.UNDER_REVIEW);

            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(candidateRepository.findAllByReserveId(RESERVE_ID))
                    .thenReturn(List.of(rejected, nominated, approved, reviewing));
            when(employeeDirectory.findEmployees(any())).thenReturn(Map.of(
                    UNASSIGNED_EMPLOYEE_ID, employee(UNASSIGNED_EMPLOYEE_ID, true, null, null),
                    UNKNOWN_REF_EMPLOYEE_ID, employee(UNKNOWN_REF_EMPLOYEE_ID, true, UNKNOWN_POSITION_ID, GRADE_ID),
                    NAMED_EMPLOYEE_ID, employee(NAMED_EMPLOYEE_ID, true, POSITION_ID, UNKNOWN_GRADE_ID)
            ));
            when(employeeDirectory.findPositions(any())).thenReturn(Map.of(
                    POSITION_ID, new ReferenceLookupItemDto(POSITION_ID, "Engineer", true)
            ));
            when(employeeDirectory.findGrades(any())).thenReturn(Map.of(
                    GRADE_ID, new ReferenceLookupItemDto(GRADE_ID, "Middle", true)
            ));

            var analysis = service.analysis(RESERVE_ID);

            assertThat(analysis.getReserveId()).isEqualTo(RESERVE_ID);
            assertThat(analysis.getStatus()).isEqualTo(TalentReserveStatus.ACTIVE);
            assertThat(analysis.getCandidateCount()).isEqualTo(4L);
            assertThat(analysis.getActiveCandidateCount()).isEqualTo(3L);
            assertThat(analysis.getApprovedCandidateCount()).isEqualTo(1L);
            assertThat(analysis.getByStatus())
                    .containsEntry("NOMINATED", 1L)
                    .containsEntry("UNDER_REVIEW", 1L)
                    .containsEntry("APPROVED", 1L)
                    .containsEntry("REJECTED", 1L)
                    .containsEntry("REMOVED", 0L);
            assertThat(analysis.getByPosition())
                    .containsEntry("Не указана", 1L)
                    .containsEntry(UNKNOWN_POSITION_ID.toString(), 1L)
                    .containsEntry("Engineer", 1L);
            assertThat(analysis.getByGrade())
                    .containsEntry("Не указан", 1L)
                    .containsEntry("Middle", 1L)
                    .containsEntry(UNKNOWN_GRADE_ID.toString(), 1L);
        }

        @Test
        @DisplayName("returns zero counts when the reserve has no candidates")
        void shouldAnalyzeEmptyReserve() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.DRAFT)));
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of());
            when(employeeDirectory.findEmployees(any())).thenReturn(Map.of());
            when(employeeDirectory.findPositions(any())).thenReturn(Map.of());
            when(employeeDirectory.findGrades(any())).thenReturn(Map.of());

            var analysis = service.analysis(RESERVE_ID);

            assertThat(analysis.getCandidateCount()).isZero();
            assertThat(analysis.getActiveCandidateCount()).isZero();
            assertThat(analysis.getApprovedCandidateCount()).isZero();
            assertThat(analysis.getByStatus().values()).containsOnly(0L);
            assertThat(analysis.getByPosition()).isEmpty();
            assertThat(analysis.getByGrade()).isEmpty();
        }
    }

    @Nested
    @DisplayName("context")
    class Context {

        @Test
        @DisplayName("returns reserve context and candidate counts without an access check")
        void shouldReturnContext() {
            when(reserveRepository.findById(RESERVE_ID)).thenReturn(Optional.of(reserve(TalentReserveStatus.ACTIVE)));
            when(candidateRepository.findAllByReserveId(RESERVE_ID)).thenReturn(List.of(
                    candidate(ReserveCandidateStatus.APPROVED),
                    candidate(ReserveCandidateStatus.REJECTED),
                    candidate(ReserveCandidateStatus.NOMINATED)
            ));

            var context = service.context(RESERVE_ID);

            assertThat(context.getReserveId()).isEqualTo(RESERVE_ID);
            assertThat(context.getName()).isEqualTo("Leadership");
            assertThat(context.getStatus()).isEqualTo(TalentReserveStatus.ACTIVE);
            assertThat(context.getTargetPositionId()).isEqualTo(POSITION_ID);
            assertThat(context.getTargetGradeId()).isEqualTo(GRADE_ID);
            assertThat(context.getEffectiveFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(context.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(context.getCandidateCount()).isEqualTo(3L);
            assertThat(context.getActiveCandidateCount()).isEqualTo(2L);
            assertThat(context.getApprovedCandidateCount()).isEqualTo(1L);
            verifyNoInteractions(accessService, employeeDirectory);
        }
    }

    private void arrangeDecision(ReserveCandidate candidate, TalentReserveStatus status) {
        when(candidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        when(reserveRepository.findByIdForUpdate(RESERVE_ID)).thenReturn(Optional.of(reserve(status)));
        when(candidateRepository.findByIdForUpdate(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        when(accessService.currentEmployeeId()).thenReturn(ACTOR_ID);
        when(decisionRepository.save(any(ReserveDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(decisionMapper.toResponseDto(any(ReserveDecision.class)))
                .thenReturn(ReserveDecisionResponseDto.builder().build());
    }

    private void stubReserveMapper() {
        when(reserveMapper.toResponseDto(any(TalentReserve.class))).thenAnswer(invocation -> {
            TalentReserve reserve = invocation.getArgument(0);
            return TalentReserveResponseDto.builder()
                    .id(reserve.getId())
                    .name(reserve.getName())
                    .status(reserve.getStatus())
                    .createdBy(reserve.getCreatedBy())
                    .build();
        });
    }

    private ReserveCandidateStatus expectedStatus(ReserveDecisionType decision) {
        return switch (decision) {
            case APPROVE -> ReserveCandidateStatus.APPROVED;
            case REJECT -> ReserveCandidateStatus.REJECTED;
            case REQUEST_REVIEW -> ReserveCandidateStatus.UNDER_REVIEW;
            case REMOVE -> ReserveCandidateStatus.REMOVED;
        };
    }

    private TalentReserve reserve(TalentReserveStatus status) {
        return TalentReserve.builder()
                .id(RESERVE_ID)
                .name("Leadership")
                .status(status)
                .version(VERSION)
                .targetPositionId(POSITION_ID)
                .targetGradeId(GRADE_ID)
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .effectiveTo(LocalDate.of(2026, 12, 31))
                .createdBy(USER_ID)
                .build();
    }

    private ReserveCandidate candidate(ReserveCandidateStatus status) {
        return candidate(EMPLOYEE_ID, status);
    }

    private ReserveCandidate candidate(UUID employeeId, ReserveCandidateStatus status) {
        return ReserveCandidate.builder()
                .id(CANDIDATE_ID)
                .reserveId(RESERVE_ID)
                .employeeId(employeeId)
                .status(status)
                .version(VERSION)
                .nominatedBy(ACTOR_ID)
                .nominatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private EmployeeLookupItemDto employee(UUID id, boolean active, UUID positionId, UUID gradeId) {
        return new EmployeeLookupItemDto(id, "Ada Lovelace", active, positionId, gradeId);
    }

    private ReserveCandidateNominationRequestDto nomination() {
        return ReserveCandidateNominationRequestDto.builder()
                .employeeId(EMPLOYEE_ID)
                .assessmentId(ASSESSMENT_ID)
                .comment("ready")
                .build();
    }

    private ReserveCandidateDecisionCreateRequestDto decision(ReserveDecisionType decision, UUID assessmentId) {
        return ReserveCandidateDecisionCreateRequestDto.builder()
                .decision(decision)
                .comment("note")
                .assessmentId(assessmentId)
                .expectedVersion(VERSION)
                .build();
    }

    private TalentReserveTransitionRequestDto transition(Long version) {
        return TalentReserveTransitionRequestDto.builder().expectedVersion(version).comment("go").build();
    }

    @SuppressWarnings("unchecked")
    private Specification<TalentReserve> anyReserveSpecification() {
        return any(Specification.class);
    }

    @SuppressWarnings("unchecked")
    private Specification<ReserveCandidate> anyCandidateSpecification() {
        return any(Specification.class);
    }

    @SuppressWarnings("unchecked")
    private Specification<ReserveDecision> anyDecisionSpecification() {
        return any(Specification.class);
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
