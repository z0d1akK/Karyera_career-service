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
import com.zodiakk.careerservice.common.constants.KafkaEventConstants;
import com.zodiakk.careerservice.common.constants.SecurityConstants;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.SortDirection;
import com.zodiakk.careerservice.common.enums.RequirementStatus;
import com.zodiakk.careerservice.common.exception.domain.DuplicateResourceException;
import com.zodiakk.careerservice.common.exception.domain.ImmutableResourceException;
import com.zodiakk.careerservice.common.exception.domain.InvalidStateTransitionException;
import com.zodiakk.careerservice.common.exception.domain.PositionRequirementNotFoundException;
import com.zodiakk.careerservice.integration.employee.service.EmployeeDirectoryService;
import com.zodiakk.careerservice.outbox.service.OutboxEventService;
import com.zodiakk.careerservice.security.access.CareerAccessService;
import com.zodiakk.careerservice.security.principal.CurrentUser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionRequirementServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID REQUIREMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID PREVIOUS_REQUIREMENT_ID = UUID.fromString("23232323-2323-2323-2323-232323232323");

    private static final UUID POSITION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID OTHER_POSITION_ID = UUID.fromString("34343434-3434-3434-3434-343434343434");

    private static final UUID GRADE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID OTHER_GRADE_ID = UUID.fromString("45454545-4545-4545-4545-454545454545");

    private static final UUID SKILL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID OTHER_SKILL_ID = UUID.fromString("56565656-5656-5656-5656-565656565656");

    private static final BigDecimal WEIGHT = new BigDecimal("1.50");

    @Mock
    private PositionRequirementRepository requirementRepository;

    @Mock
    private RequiredSkillRepository requiredSkillRepository;

    @Mock
    private PositionRequirementMapper requirementMapper;

    @Mock
    private RequiredSkillMapper requiredSkillMapper;

    @Mock
    private EmployeeDirectoryService employeeDirectory;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private CareerAccessService accessService;

    @InjectMocks
    private PositionRequirementService positionRequirementService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("list")
    class ListRequirements {

        @Test
        @DisplayName("maps a filtered page and passes the filter through")
        void shouldListRequirements() {
            PositionRequirementFilterDto filter = PositionRequirementFilterDto.builder()
                    .positionId(POSITION_ID)
                    .gradeId(GRADE_ID)
                    .status(RequirementStatus.PUBLISHED)
                    .build();
            PageRequestDto pageRequest = PageRequestDto.builder()
                    .page(1)
                    .size(5)
                    .sortBy("revision")
                    .sortDirection(SortDirection.DESC)
                    .build();
            PositionRequirement requirement = requirement(RequirementStatus.PUBLISHED, 2);
            PositionRequirementResponseDto response = response(requirement);
            when(requirementRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(requirement), PageRequest.of(1, 5), 6));
            when(requirementMapper.toResponseDto(requirement)).thenReturn(response);
            when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID)).thenReturn(List.of());

            Page<PositionRequirementResponseDto> result = positionRequirementService.list(filter, pageRequest);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(response.getSkills()).isEmpty();
            verifyNoInteractions(accessService);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Specification<PositionRequirement>> specificationCaptor =
                    ArgumentCaptor.forClass(Specification.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(requirementRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("revision").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertSpecificationFiltersPosition(specificationCaptor.getValue());
        }

        @Test
        @DisplayName("passes a null filter through as an unrestricted query")
        void shouldAcceptNullFilter() {
            when(requirementRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(positionRequirementService.list(null, pageRequest())).isEmpty();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Specification<PositionRequirement>> specificationCaptor =
                    ArgumentCaptor.forClass(Specification.class);
            verify(requirementRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

            Root<PositionRequirement> root = mock(Root.class);
            CriteriaQuery<PositionRequirement> query = mock(CriteriaQuery.class);
            CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
            Predicate predicate = mock(Predicate.class);
            when(criteriaBuilder.conjunction()).thenReturn(predicate);

            assertThat(specificationCaptor.getValue().toPredicate(root, query, criteriaBuilder)).isSameAs(predicate);
            verifyNoInteractions(requirementMapper, requiredSkillMapper);
        }

        @Test
        @DisplayName("returns an empty page without calling the mapper")
        void shouldReturnEmptyPage() {
            when(requirementRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            Page<PositionRequirementResponseDto> result = positionRequirementService.list(
                    PositionRequirementFilterDto.builder().status(RequirementStatus.DRAFT).build(),
                    pageRequest()
            );

            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verifyNoInteractions(requirementMapper, requiredSkillRepository);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("starts revision 1 when the position and grade have no requirements")
        void shouldCreateFirstRevisionWithoutSkills() {
            when(requirementRepository.findTopByPositionIdAndGradeIdOrderByRevisionDesc(POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.empty());
            when(requirementRepository.save(any(PositionRequirement.class))).thenAnswer(invocation -> {
                PositionRequirement saved = invocation.getArgument(0);
                saved.setId(REQUIREMENT_ID);
                return saved;
            });
            PositionRequirementResponseDto response = stubResponse();

            PositionRequirementResponseDto result = positionRequirementService.create(request(null));

            assertThat(result).isSameAs(response);
            assertThat(result.getSkills()).isEmpty();
            PositionRequirement saved = captureSavedRequirement();
            assertThat(saved.getPositionId()).isEqualTo(POSITION_ID);
            assertThat(saved.getGradeId()).isEqualTo(GRADE_ID);
            assertThat(saved.getRevision()).isEqualTo(1);
            assertThat(saved.getStatus()).isEqualTo(RequirementStatus.DRAFT);
            assertThat(saved.getDescription()).isEqualTo("Backend requirements");
            assertThat(saved.getMinExperienceMonths()).isEqualTo(24);
            verify(employeeDirectory).requirePosition(POSITION_ID);
            verify(employeeDirectory).requireGrade(GRADE_ID);
            verify(requiredSkillRepository).deleteAllByRequirementId(REQUIREMENT_ID);
            verify(requiredSkillRepository, never()).save(any());
            verify(employeeDirectory, never()).requireSkill(any());
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("clears skills when the request contains an empty list")
        void shouldAcceptEmptySkillList() {
            when(requirementRepository.findTopByPositionIdAndGradeIdOrderByRevisionDesc(POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.empty());
            when(requirementRepository.save(any(PositionRequirement.class))).thenAnswer(invocation -> {
                PositionRequirement saved = invocation.getArgument(0);
                saved.setId(REQUIREMENT_ID);
                return saved;
            });
            stubResponse();

            positionRequirementService.create(request(List.of()));

            verify(requiredSkillRepository).deleteAllByRequirementId(REQUIREMENT_ID);
            verify(requiredSkillRepository, never()).save(any());
        }

        @Test
        @DisplayName("increments the revision and replaces skills")
        void shouldCreateNextRevisionWithSkills() {
            PositionRequirement existing = requirement(RequirementStatus.PUBLISHED, 2);
            when(requirementRepository.findTopByPositionIdAndGradeIdOrderByRevisionDesc(POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.of(existing));
            when(requirementRepository.save(any(PositionRequirement.class))).thenAnswer(invocation -> {
                PositionRequirement saved = invocation.getArgument(0);
                saved.setId(REQUIREMENT_ID);
                return saved;
            });
            RequiredSkillResponseDto skillResponse = RequiredSkillResponseDto.builder().skillId(SKILL_ID).build();
            when(requirementMapper.toResponseDto(any(PositionRequirement.class))).thenAnswer(invocation -> {
                PositionRequirement requirement = invocation.getArgument(0);
                return PositionRequirementResponseDto.builder()
                        .id(requirement.getId())
                        .revision(requirement.getRevision())
                        .status(requirement.getStatus())
                        .build();
            });
            when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID))
                    .thenReturn(List.of(storedSkill(SKILL_ID), storedSkill(OTHER_SKILL_ID)));
            when(requiredSkillMapper.toResponseDto(any(RequiredSkill.class))).thenReturn(skillResponse);

            PositionRequirementResponseDto result = positionRequirementService.create(request(List.of(
                    skill(SKILL_ID, true),
                    skill(OTHER_SKILL_ID, false)
            )));

            assertThat(captureSavedRequirement().getRevision()).isEqualTo(3);
            assertThat(result.getRevision()).isEqualTo(3);
            assertThat(result.getSkills()).containsExactly(skillResponse, skillResponse);

            ArgumentCaptor<RequiredSkill> skillCaptor = ArgumentCaptor.forClass(RequiredSkill.class);
            verify(requiredSkillRepository, times(2)).save(skillCaptor.capture());
            RequiredSkill first = skillCaptor.getAllValues().get(0);
            assertThat(first.getRequirementId()).isEqualTo(REQUIREMENT_ID);
            assertThat(first.getSkillId()).isEqualTo(SKILL_ID);
            assertThat(first.getRequiredLevel()).isEqualTo(3);
            assertThat(first.getWeight()).isEqualByComparingTo(WEIGHT);
            assertThat(first.getMandatory()).isTrue();
            assertThat(first.getDescription()).isEqualTo("Java");
            assertThat(skillCaptor.getAllValues().get(1).getSkillId()).isEqualTo(OTHER_SKILL_ID);
            assertThat(skillCaptor.getAllValues().get(1).getMandatory()).isFalse();
            verify(employeeDirectory).requireSkill(SKILL_ID);
            verify(employeeDirectory).requireSkill(OTHER_SKILL_ID);
        }

        @Test
        @DisplayName("rejects a duplicated skill after saving the first one")
        void shouldRejectDuplicateSkill() {
            when(requirementRepository.findTopByPositionIdAndGradeIdOrderByRevisionDesc(POSITION_ID, GRADE_ID))
                    .thenReturn(Optional.empty());
            when(requirementRepository.save(any(PositionRequirement.class))).thenAnswer(invocation -> {
                PositionRequirement saved = invocation.getArgument(0);
                saved.setId(REQUIREMENT_ID);
                return saved;
            });

            assertThatThrownBy(() -> positionRequirementService.create(request(List.of(
                    skill(SKILL_ID, true),
                    skill(SKILL_ID, false)
            )))).isInstanceOf(DuplicateResourceException.class);

            verify(requiredSkillRepository).deleteAllByRequirementId(REQUIREMENT_ID);
            verify(requiredSkillRepository, times(1)).save(any());
            verify(employeeDirectory, times(1)).requireSkill(SKILL_ID);
            verify(requiredSkillRepository, never()).findAllByRequirementId(any());
            verifyNoInteractions(requirementMapper);
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> positionRequirementService.create(request(null)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(employeeDirectory, requirementRepository);
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns the requirement and its skills")
        void shouldReturnRequirement() {
            PositionRequirement requirement = requirement(RequirementStatus.DRAFT, 1);
            RequiredSkill skill = storedSkill(SKILL_ID);
            RequiredSkillResponseDto skillResponse = RequiredSkillResponseDto.builder().id(skill.getId()).build();
            PositionRequirementResponseDto response = response(requirement);
            when(requirementRepository.findById(REQUIREMENT_ID)).thenReturn(Optional.of(requirement));
            when(requirementMapper.toResponseDto(requirement)).thenReturn(response);
            when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID)).thenReturn(List.of(skill));
            when(requiredSkillMapper.toResponseDto(skill)).thenReturn(skillResponse);

            PositionRequirementResponseDto result = positionRequirementService.get(REQUIREMENT_ID);

            assertThat(result).isSameAs(response);
            assertThat(result.getSkills()).containsExactly(skillResponse);
            verifyNoInteractions(accessService);
        }

        @Test
        @DisplayName("throws when the requirement does not exist")
        void shouldThrowWhenMissing() {
            when(requirementRepository.findById(REQUIREMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> positionRequirementService.get(REQUIREMENT_ID))
                    .isInstanceOf(PositionRequirementNotFoundException.class)
                    .hasMessageContaining(REQUIREMENT_ID.toString());

            verifyNoInteractions(requirementMapper, requiredSkillRepository);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates a draft and replaces its skills")
        void shouldUpdateDraft() {
            PositionRequirement requirement = requirement(RequirementStatus.DRAFT, 1);
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID)).thenReturn(Optional.of(requirement));
            stubResponse();

            PositionRequirementResponseDto result = positionRequirementService.update(
                    REQUIREMENT_ID,
                    request(List.of(skill(SKILL_ID, true)))
            );

            assertThat(requirement.getDescription()).isEqualTo("Backend requirements");
            assertThat(requirement.getMinExperienceMonths()).isEqualTo(24);
            assertThat(requirement.getPositionId()).isEqualTo(POSITION_ID);
            assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.DRAFT);
            assertThat(result.getId()).isEqualTo(REQUIREMENT_ID);
            verify(requirementRepository).saveAndFlush(requirement);
            verify(requiredSkillRepository).deleteAllByRequirementId(REQUIREMENT_ID);
            verify(employeeDirectory).requireSkill(SKILL_ID);
            verify(requiredSkillRepository).save(any(RequiredSkill.class));
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("removes every skill when the request omits them")
        void shouldClearSkillsWhenOmitted() {
            PositionRequirement requirement = requirement(RequirementStatus.DRAFT, 1);
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID)).thenReturn(Optional.of(requirement));
            stubResponse();

            positionRequirementService.update(REQUIREMENT_ID, request(null));

            verify(requiredSkillRepository).deleteAllByRequirementId(REQUIREMENT_ID);
            verify(requiredSkillRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = RequirementStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
        @DisplayName("rejects a requirement that is not a draft")
        void shouldRejectNonDraft(RequirementStatus status) {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID))
                    .thenReturn(Optional.of(requirement(status, 1)));

            assertThatThrownBy(() -> positionRequirementService.update(REQUIREMENT_ID, request(null)))
                    .isInstanceOf(ImmutableResourceException.class);

            verify(requirementRepository, never()).saveAndFlush(any());
            verifyNoInteractions(employeeDirectory, requiredSkillRepository);
        }

        @Test
        @DisplayName("rejects a change of position")
        void shouldRejectPositionChange() {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID))
                    .thenReturn(Optional.of(requirement(RequirementStatus.DRAFT, 1)));
            PositionRequirementUpsertRequestDto request = request(null);
            request.setPositionId(OTHER_POSITION_ID);

            assertThatThrownBy(() -> positionRequirementService.update(REQUIREMENT_ID, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Позиция и грейд версии требований не могут быть изменены");

            verifyNoInteractions(employeeDirectory);
            verify(requirementRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("rejects a change of grade")
        void shouldRejectGradeChange() {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID))
                    .thenReturn(Optional.of(requirement(RequirementStatus.DRAFT, 1)));
            PositionRequirementUpsertRequestDto request = request(null);
            request.setGradeId(OTHER_GRADE_ID);

            assertThatThrownBy(() -> positionRequirementService.update(REQUIREMENT_ID, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Позиция и грейд версии требований не могут быть изменены");

            verifyNoInteractions(employeeDirectory);
        }

        @Test
        @DisplayName("throws when the requirement does not exist")
        void shouldThrowWhenMissing() {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> positionRequirementService.update(REQUIREMENT_ID, request(null)))
                    .isInstanceOf(PositionRequirementNotFoundException.class)
                    .hasMessageContaining(REQUIREMENT_ID.toString());
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> positionRequirementService.update(REQUIREMENT_ID, request(null)))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(requirementRepository);
        }
    }

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("supersedes the previous published revision and emits an outbox event")
        void shouldSupersedePreviousRevision() {
            authenticate();
            OffsetDateTime before = OffsetDateTime.now().minusSeconds(2);
            PositionRequirement requirement = requirement(RequirementStatus.DRAFT, 3);
            PositionRequirement previous = requirement(RequirementStatus.PUBLISHED, 2);
            previous.setId(PREVIOUS_REQUIREMENT_ID);
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID)).thenReturn(Optional.of(requirement));
            when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID))
                    .thenReturn(List.of(storedSkill(SKILL_ID)));
            when(requirementRepository.findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
                    POSITION_ID,
                    GRADE_ID,
                    RequirementStatus.PUBLISHED
            )).thenReturn(Optional.of(previous));
            PositionRequirementResponseDto response = stubMapper();
            when(requiredSkillMapper.toResponseDto(any(RequiredSkill.class)))
                    .thenReturn(RequiredSkillResponseDto.builder().skillId(SKILL_ID).build());

            PositionRequirementResponseDto result = positionRequirementService.publish(REQUIREMENT_ID);

            assertThat(result).isSameAs(response);
            assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.PUBLISHED);
            assertThat(requirement.getPublishedBy()).isEqualTo(USER_ID);
            assertThat(requirement.getPublishedAt()).isAfter(before);
            assertThat(previous.getStatus()).isEqualTo(RequirementStatus.SUPERSEDED);
            verify(requirementRepository).saveAndFlush(previous);
            verify(requirementRepository).saveAndFlush(requirement);
            verify(outboxEventService).saveEvent(
                    eq(REQUIREMENT_ID),
                    eq(KafkaEventConstants.AGGREGATE_TYPE_POSITION_REQUIREMENT),
                    eq(KafkaEventConstants.POSITION_REQUIREMENT_PUBLISHED),
                    eq(KafkaEventConstants.EVENT_VERSION_V1),
                    eq(Map.of(
                            "requirementId", REQUIREMENT_ID,
                            "positionId", POSITION_ID,
                            "gradeId", GRADE_ID,
                            "revision", 3
                    ))
            );
        }

        @Test
        @DisplayName("publishes the first revision when nothing is currently published")
        void shouldPublishWithoutPreviousRevision() {
            authenticate();
            PositionRequirement requirement = requirement(RequirementStatus.DRAFT, 1);
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID)).thenReturn(Optional.of(requirement));
            when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID))
                    .thenReturn(List.of(storedSkill(SKILL_ID)));
            when(requirementRepository.findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
                    POSITION_ID,
                    GRADE_ID,
                    RequirementStatus.PUBLISHED
            )).thenReturn(Optional.empty());
            stubMapper();
            when(requiredSkillMapper.toResponseDto(any(RequiredSkill.class)))
                    .thenReturn(RequiredSkillResponseDto.builder().skillId(SKILL_ID).build());

            positionRequirementService.publish(REQUIREMENT_ID);

            assertThat(requirement.getStatus()).isEqualTo(RequirementStatus.PUBLISHED);
            assertThat(requirement.getPublishedBy()).isEqualTo(USER_ID);
            verify(requirementRepository, times(1)).saveAndFlush(any());
        }

        @Test
        @DisplayName("rejects a requirement that has no skills")
        void shouldRejectRequirementWithoutSkills() {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID))
                    .thenReturn(Optional.of(requirement(RequirementStatus.DRAFT, 1)));
            when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> positionRequirementService.publish(REQUIREMENT_ID))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessage("Требования к должности без навыков не могут быть опубликованы");

            verify(requirementRepository, never())
                    .findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(any(), any(), any());
            verifyNoInteractions(outboxEventService);
        }

        @ParameterizedTest
        @EnumSource(value = RequirementStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
        @DisplayName("rejects a requirement that is not a draft")
        void shouldRejectNonDraft(RequirementStatus status) {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID))
                    .thenReturn(Optional.of(requirement(status, 1)));

            assertThatThrownBy(() -> positionRequirementService.publish(REQUIREMENT_ID))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining(status.name())
                    .hasMessageContaining(RequirementStatus.PUBLISHED.name());

            verifyNoInteractions(requiredSkillRepository, outboxEventService);
        }

        @Test
        @DisplayName("throws when the requirement does not exist")
        void shouldThrowWhenMissing() {
            when(requirementRepository.findByIdForUpdate(REQUIREMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> positionRequirementService.publish(REQUIREMENT_ID))
                    .isInstanceOf(PositionRequirementNotFoundException.class)
                    .hasMessageContaining(REQUIREMENT_ID.toString());
        }

        @Test
        @DisplayName("denies a user without HR rights")
        void shouldDenyWithoutHrRights() {
            denyHr();

            assertThatThrownBy(() -> positionRequirementService.publish(REQUIREMENT_ID))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(requirementRepository);
        }
    }

    @Nested
    @DisplayName("current")
    class Current {

        @Test
        @DisplayName("returns the latest published requirement")
        void shouldReturnCurrentRequirement() {
            PositionRequirement requirement = requirement(RequirementStatus.PUBLISHED, 4);
            PositionRequirementResponseDto response = stubLoadedResponse(requirement);
            when(requirementRepository.findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
                    POSITION_ID,
                    GRADE_ID,
                    RequirementStatus.PUBLISHED
            )).thenReturn(Optional.of(requirement));

            assertThat(positionRequirementService.current(POSITION_ID, GRADE_ID)).isSameAs(response);
            verifyNoInteractions(accessService);
        }

        @Test
        @DisplayName("throws when nothing is published for the position and grade")
        void shouldThrowWhenMissing() {
            when(requirementRepository.findFirstByPositionIdAndGradeIdAndStatusOrderByRevisionDesc(
                    POSITION_ID,
                    GRADE_ID,
                    RequirementStatus.PUBLISHED
            )).thenReturn(Optional.empty());

            assertThatThrownBy(() -> positionRequirementService.current(POSITION_ID, GRADE_ID))
                    .isInstanceOf(PositionRequirementNotFoundException.class)
                    .hasMessageContaining(POSITION_ID.toString())
                    .hasMessageContaining(GRADE_ID.toString());

            verifyNoInteractions(requirementMapper);
        }
    }

    @Nested
    @DisplayName("history")
    class History {

        @Test
        @DisplayName("preserves repository order and maps every revision")
        void shouldReturnHistory() {
            PositionRequirement newer = requirement(RequirementStatus.PUBLISHED, 2);
            PositionRequirement older = requirement(RequirementStatus.SUPERSEDED, 1);
            older.setId(PREVIOUS_REQUIREMENT_ID);
            PositionRequirementResponseDto newerResponse = response(newer);
            PositionRequirementResponseDto olderResponse = response(older);
            when(requirementRepository.findAllByPositionIdAndGradeIdOrderByRevisionDesc(POSITION_ID, GRADE_ID))
                    .thenReturn(List.of(newer, older));
            when(requirementMapper.toResponseDto(newer)).thenReturn(newerResponse);
            when(requirementMapper.toResponseDto(older)).thenReturn(olderResponse);
            when(requiredSkillRepository.findAllByRequirementId(any())).thenReturn(List.of());

            assertThat(positionRequirementService.history(POSITION_ID, GRADE_ID))
                    .containsExactly(newerResponse, olderResponse);
            verifyNoInteractions(accessService);
        }

        @Test
        @DisplayName("returns an empty history without calling the mapper")
        void shouldReturnEmptyHistory() {
            when(requirementRepository.findAllByPositionIdAndGradeIdOrderByRevisionDesc(POSITION_ID, GRADE_ID))
                    .thenReturn(List.of());

            assertThat(positionRequirementService.history(POSITION_ID, GRADE_ID)).isEmpty();

            verifyNoInteractions(requirementMapper, requiredSkillRepository);
        }
    }

    private PositionRequirement captureSavedRequirement() {
        ArgumentCaptor<PositionRequirement> captor = ArgumentCaptor.forClass(PositionRequirement.class);
        verify(requirementRepository).save(captor.capture());
        return captor.getValue();
    }

    private PositionRequirementResponseDto stubResponse() {
        when(requiredSkillRepository.findAllByRequirementId(REQUIREMENT_ID)).thenReturn(List.of());
        return stubMapper();
    }

    private PositionRequirementResponseDto stubLoadedResponse(PositionRequirement requirement) {
        when(requiredSkillRepository.findAllByRequirementId(requirement.getId())).thenReturn(List.of());
        PositionRequirementResponseDto response = response(requirement);
        when(requirementMapper.toResponseDto(requirement)).thenReturn(response);
        return response;
    }

    private PositionRequirementResponseDto stubMapper() {
        PositionRequirementResponseDto response = PositionRequirementResponseDto.builder()
                .id(REQUIREMENT_ID)
                .build();
        when(requirementMapper.toResponseDto(any(PositionRequirement.class))).thenAnswer(invocation -> {
            PositionRequirement requirement = invocation.getArgument(0);
            response.setId(requirement.getId());
            response.setRevision(requirement.getRevision());
            response.setStatus(requirement.getStatus());
            return response;
        });
        return response;
    }

    @SuppressWarnings("unchecked")
    private void assertSpecificationFiltersPosition(Specification<PositionRequirement> specification) {
        Root<PositionRequirement> root = mock(Root.class);
        CriteriaQuery<PositionRequirement> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<Object> positionPath = mock(Path.class);
        when(root.get("positionId")).thenReturn(positionPath);

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(positionPath, POSITION_ID);
    }

    private PositionRequirement requirement(RequirementStatus status, int revision) {
        return PositionRequirement.builder()
                .id(REQUIREMENT_ID)
                .positionId(POSITION_ID)
                .gradeId(GRADE_ID)
                .revision(revision)
                .status(status)
                .description("Old")
                .minExperienceMonths(12)
                .build();
    }

    private PositionRequirementResponseDto response(PositionRequirement requirement) {
        return PositionRequirementResponseDto.builder()
                .id(requirement.getId())
                .positionId(requirement.getPositionId())
                .gradeId(requirement.getGradeId())
                .revision(requirement.getRevision())
                .status(requirement.getStatus())
                .build();
    }

    private RequiredSkill storedSkill(UUID skillId) {
        return RequiredSkill.builder()
                .id(UUID.fromString("67676767-6767-6767-6767-676767676767"))
                .requirementId(REQUIREMENT_ID)
                .skillId(skillId)
                .requiredLevel(3)
                .weight(WEIGHT)
                .mandatory(true)
                .description("Java")
                .build();
    }

    private PositionRequirementUpsertRequestDto request(List<RequiredSkillItemDto> skills) {
        return PositionRequirementUpsertRequestDto.builder()
                .positionId(POSITION_ID)
                .gradeId(GRADE_ID)
                .description("Backend requirements")
                .minExperienceMonths(24)
                .skills(skills)
                .build();
    }

    private RequiredSkillItemDto skill(UUID skillId, boolean mandatory) {
        return RequiredSkillItemDto.builder()
                .skillId(skillId)
                .requiredLevel(3)
                .weight(WEIGHT)
                .mandatory(mandatory)
                .description("Java")
                .build();
    }

    private PageRequestDto pageRequest() {
        return PageRequestDto.builder()
                .page(0)
                .size(20)
                .sortBy("revision")
                .sortDirection(SortDirection.DESC)
                .build();
    }

    private void denyHr() {
        doThrow(new AccessDeniedException("Недостаточно прав для управления карьерными справочниками"))
                .when(accessService).requireHrOrAdmin();
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
