package com.zodiakk.careerservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KafkaEventConstants {

    public static final int EVENT_VERSION_V1 = 1;

    public static final String DLT_SUFFIX = ".DLT";

    public static final String AGGREGATE_TYPE_CAREER_TRACK = "CAREER_TRACK";

    public static final String AGGREGATE_TYPE_POSITION_REQUIREMENT = "POSITION_REQUIREMENT";

    public static final String AGGREGATE_TYPE_CAREER_GOAL = "CAREER_GOAL";

    public static final String AGGREGATE_TYPE_DEVELOPMENT_PLAN = "DEVELOPMENT_PLAN";

    public static final String AGGREGATE_TYPE_DEVELOPMENT_ACTIVITY = "DEVELOPMENT_ACTIVITY";

    public static final String AGGREGATE_TYPE_RESERVE_CANDIDATE = "RESERVE_CANDIDATE";

    public static final String CAREER_TRACK_PUBLISHED = "CareerTrackPublished";

    public static final String POSITION_REQUIREMENT_PUBLISHED = "PositionRequirementPublished";

    public static final String CAREER_GOAL_CHANGED = "CareerGoalChanged";

    public static final String DEVELOPMENT_PLAN_APPROVED = "DevelopmentPlanApproved";

    public static final String DEVELOPMENT_ACTIVITY_COMPLETED = "DevelopmentActivityCompleted";

    public static final String TALENT_RESERVE_CANDIDATE_APPROVED = "TalentReserveCandidateApproved";

    public static final String EMPLOYEE_SKILL_UPDATED = "EmployeeSkillUpdated";

    public static final String EMPLOYEE_POSITION_CHANGED = "EmployeePositionChanged";

    public static final String CAREER_ASSESSMENT_COMPLETED = "CareerAssessmentCompleted";

    public static final String TALENT_RESERVE_SCREENING_COMPLETED = "TalentReserveScreeningCompleted";
}
