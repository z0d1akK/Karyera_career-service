package com.zodiakk.careerservice.repository;

import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackFilterDto;
import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackNodeFilterDto;
import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackRevisionFilterDto;
import com.zodiakk.careerservice.career.dto.request.filter.CareerTransitionFilterDto;
import com.zodiakk.careerservice.career.dto.request.filter.EmployeeCareerGoalFilterDto;
import com.zodiakk.careerservice.career.dto.request.filter.PositionRequirementFilterDto;
import com.zodiakk.careerservice.career.dto.request.filter.RequiredSkillFilterDto;
import com.zodiakk.careerservice.career.repository.CareerTrackNodeRepository;
import com.zodiakk.careerservice.career.repository.CareerTrackRepository;
import com.zodiakk.careerservice.career.repository.CareerTrackRevisionRepository;
import com.zodiakk.careerservice.career.repository.CareerTransitionRepository;
import com.zodiakk.careerservice.career.repository.EmployeeCareerGoalRepository;
import com.zodiakk.careerservice.career.repository.PositionRequirementRepository;
import com.zodiakk.careerservice.career.repository.RequiredSkillRepository;
import com.zodiakk.careerservice.career.repository.specification.careerTrack.CareerTrackSpecification;
import com.zodiakk.careerservice.career.repository.specification.careerTrackNode.CareerTrackNodeSpecification;
import com.zodiakk.careerservice.career.repository.specification.careerTrackRevision.CareerTrackRevisionSpecification;
import com.zodiakk.careerservice.career.repository.specification.careerTransition.CareerTransitionSpecification;
import com.zodiakk.careerservice.career.repository.specification.employeeCareerGoal.EmployeeCareerGoalSpecification;
import com.zodiakk.careerservice.career.repository.specification.positionRequirement.PositionRequirementSpecification;
import com.zodiakk.careerservice.career.repository.specification.requiredSkill.RequiredSkillSpecification;
import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.enums.CareerGoalStatus;
import com.zodiakk.careerservice.common.enums.CareerRevisionStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackType;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityStatus;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityType;
import com.zodiakk.careerservice.common.enums.DevelopmentPlanStatus;
import com.zodiakk.careerservice.common.enums.EvidenceVerificationStatus;
import com.zodiakk.careerservice.common.enums.PlanReviewDecision;
import com.zodiakk.careerservice.common.enums.RequirementStatus;
import com.zodiakk.careerservice.common.enums.ReserveCandidateStatus;
import com.zodiakk.careerservice.common.enums.ReserveDecisionType;
import com.zodiakk.careerservice.common.enums.TalentReserveStatus;
import com.zodiakk.careerservice.development.dto.request.filter.ActivityEvidenceFilterDto;
import com.zodiakk.careerservice.development.dto.request.filter.DevelopmentActivityFilterDto;
import com.zodiakk.careerservice.development.dto.request.filter.DevelopmentPlanFilterDto;
import com.zodiakk.careerservice.development.dto.request.filter.PlanReviewFilterDto;
import com.zodiakk.careerservice.development.repository.ActivityEvidenceRepository;
import com.zodiakk.careerservice.development.repository.DevelopmentActivityRepository;
import com.zodiakk.careerservice.development.repository.DevelopmentPlanRepository;
import com.zodiakk.careerservice.development.repository.PlanReviewRepository;
import com.zodiakk.careerservice.development.repository.specification.activityEvidence.ActivityEvidenceSpecification;
import com.zodiakk.careerservice.development.repository.specification.developmentActivity.DevelopmentActivitySpecification;
import com.zodiakk.careerservice.development.repository.specification.developmentPlan.DevelopmentPlanSpecification;
import com.zodiakk.careerservice.development.repository.specification.planReview.PlanReviewSpecification;
import com.zodiakk.careerservice.talent.dto.request.filter.ReserveCandidateFilterDto;
import com.zodiakk.careerservice.talent.dto.request.filter.ReserveDecisionFilterDto;
import com.zodiakk.careerservice.talent.dto.request.filter.TalentReserveFilterDto;
import com.zodiakk.careerservice.talent.repository.ReserveCandidateRepository;
import com.zodiakk.careerservice.talent.repository.ReserveDecisionRepository;
import com.zodiakk.careerservice.talent.repository.TalentReserveRepository;
import com.zodiakk.careerservice.talent.repository.specification.reserveCandidate.ReserveCandidateSpecification;
import com.zodiakk.careerservice.talent.repository.specification.reserveDecision.ReserveDecisionSpecification;
import com.zodiakk.careerservice.talent.repository.specification.talentReserve.TalentReserveSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Specifications")
class SpecificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CareerTrackRepository careerTrackRepository;

    @Autowired
    private CareerTrackRevisionRepository careerTrackRevisionRepository;

    @Autowired
    private CareerTrackNodeRepository careerTrackNodeRepository;

    @Autowired
    private CareerTransitionRepository careerTransitionRepository;

    @Autowired
    private PositionRequirementRepository positionRequirementRepository;

    @Autowired
    private RequiredSkillRepository requiredSkillRepository;

    @Autowired
    private EmployeeCareerGoalRepository employeeCareerGoalRepository;

    @Autowired
    private DevelopmentPlanRepository developmentPlanRepository;

    @Autowired
    private DevelopmentActivityRepository developmentActivityRepository;

    @Autowired
    private ActivityEvidenceRepository activityEvidenceRepository;

    @Autowired
    private PlanReviewRepository planReviewRepository;

    @Autowired
    private TalentReserveRepository talentReserveRepository;

    @Autowired
    private ReserveCandidateRepository reserveCandidateRepository;

    @Autowired
    private ReserveDecisionRepository reserveDecisionRepository;

    @Test
    @DisplayName("queries every specification with a null filter")
    void shouldQueryNullFilters() {
        assertThat(careerTrackRepository.findAll(CareerTrackSpecification.filter(null))).isEmpty();
        assertThat(careerTrackRevisionRepository.findAll(CareerTrackRevisionSpecification.filter(null))).isEmpty();
        assertThat(careerTrackNodeRepository.findAll(CareerTrackNodeSpecification.filter(null))).isEmpty();
        assertThat(careerTransitionRepository.findAll(CareerTransitionSpecification.filter(null))).isEmpty();
        assertThat(positionRequirementRepository.findAll(PositionRequirementSpecification.filter(null))).isEmpty();
        assertThat(requiredSkillRepository.findAll(RequiredSkillSpecification.filter(null))).isEmpty();
        assertThat(employeeCareerGoalRepository.findAll(EmployeeCareerGoalSpecification.filter(null))).isEmpty();
        assertThat(developmentPlanRepository.findAll(DevelopmentPlanSpecification.filter(null))).isEmpty();
        assertThat(developmentActivityRepository.findAll(DevelopmentActivitySpecification.filter(null))).isEmpty();
        assertThat(activityEvidenceRepository.findAll(ActivityEvidenceSpecification.filter(null))).isEmpty();
        assertThat(planReviewRepository.findAll(PlanReviewSpecification.filter(null))).isEmpty();
        assertThat(talentReserveRepository.findAll(TalentReserveSpecification.filter(null))).isEmpty();
        assertThat(reserveCandidateRepository.findAll(ReserveCandidateSpecification.filter(null))).isEmpty();
        assertThat(reserveDecisionRepository.findAll(ReserveDecisionSpecification.filter(null))).isEmpty();
    }

    @Test
    @DisplayName("queries every specification with every filter field set")
    void shouldQueryPopulatedFilters() {
        UUID id = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        assertThat(careerTrackRepository.findAll(CareerTrackSpecification.filter(CareerTrackFilterDto.builder()
                .code("BE")
                .name("Backend")
                .type(CareerTrackType.VERTICAL)
                .status(CareerTrackStatus.PUBLISHED)
                .build()))).isEmpty();

        assertThat(careerTrackRevisionRepository.findAll(CareerTrackRevisionSpecification.filter(
                CareerTrackRevisionFilterDto.builder()
                        .trackId(id)
                        .status(CareerRevisionStatus.PUBLISHED)
                        .build()
        ))).isEmpty();

        assertThat(careerTrackNodeRepository.findAll(CareerTrackNodeSpecification.filter(
                CareerTrackNodeFilterDto.builder()
                        .trackId(id)
                        .positionId(id)
                        .gradeId(id)
                        .isEntryPoint(true)
                        .build()
        ))).isEmpty();

        assertThat(careerTransitionRepository.findAll(CareerTransitionSpecification.filter(
                CareerTransitionFilterDto.builder()
                        .sourceNodeId(id)
                        .targetNodeId(id)
                        .active(true)
                        .build()
        ))).isEmpty();

        assertThat(positionRequirementRepository.findAll(PositionRequirementSpecification.filter(
                PositionRequirementFilterDto.builder()
                        .positionId(id)
                        .gradeId(id)
                        .status(RequirementStatus.PUBLISHED)
                        .build()
        ))).isEmpty();

        assertThat(requiredSkillRepository.findAll(RequiredSkillSpecification.filter(RequiredSkillFilterDto.builder()
                .requirementId(id)
                .skillId(id)
                .mandatory(true)
                .build()))).isEmpty();

        assertThat(employeeCareerGoalRepository.findAll(EmployeeCareerGoalSpecification.filter(
                EmployeeCareerGoalFilterDto.builder()
                        .employeeId(id)
                        .targetPositionId(id)
                        .targetGradeId(id)
                        .status(CareerGoalStatus.ACTIVE)
                        .build()
        ))).isEmpty();

        assertThat(developmentPlanRepository.findAll(DevelopmentPlanSpecification.filter(
                DevelopmentPlanFilterDto.builder()
                        .employeeId(id)
                        .careerGoalId(id)
                        .status(DevelopmentPlanStatus.ACTIVE)
                        .startDateFrom(from)
                        .startDateTo(to)
                        .targetDateFrom(from)
                        .targetDateTo(to)
                        .build()
        ))).isEmpty();

        assertThat(developmentActivityRepository.findAll(DevelopmentActivitySpecification.filter(
                DevelopmentActivityFilterDto.builder()
                        .planId(id)
                        .skillId(id)
                        .activityType(DevelopmentActivityType.TRAINING)
                        .status(DevelopmentActivityStatus.PLANNED)
                        .dueDateFrom(from)
                        .dueDateTo(to)
                        .build()
        ))).isEmpty();

        assertThat(activityEvidenceRepository.findAll(ActivityEvidenceSpecification.filter(
                ActivityEvidenceFilterDto.builder()
                        .activityId(id)
                        .verificationStatus(EvidenceVerificationStatus.PENDING)
                        .build()
        ))).isEmpty();

        assertThat(planReviewRepository.findAll(PlanReviewSpecification.filter(PlanReviewFilterDto.builder()
                .planId(id)
                .reviewerEmployeeId(id)
                .decision(PlanReviewDecision.APPROVED)
                .build()))).isEmpty();

        assertThat(talentReserveRepository.findAll(TalentReserveSpecification.filter(TalentReserveFilterDto.builder()
                .name("Leadership")
                .targetPositionId(id)
                .targetGradeId(id)
                .status(TalentReserveStatus.ACTIVE)
                .build()))).isEmpty();

        assertThat(reserveCandidateRepository.findAll(ReserveCandidateSpecification.filter(
                ReserveCandidateFilterDto.builder()
                        .reserveId(id)
                        .employeeId(id)
                        .status(ReserveCandidateStatus.NOMINATED)
                        .build()
        ))).isEmpty();

        assertThat(reserveDecisionRepository.findAll(ReserveDecisionSpecification.filter(
                ReserveDecisionFilterDto.builder()
                        .candidateId(id)
                        .decision(ReserveDecisionType.APPROVE)
                        .build()
        ))).isEmpty();
    }
}
