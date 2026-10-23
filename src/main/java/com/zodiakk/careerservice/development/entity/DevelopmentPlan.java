package com.zodiakk.careerservice.development.entity;

import com.zodiakk.careerservice.common.entity.VersionedEntity;
import com.zodiakk.careerservice.common.enums.DevelopmentPlanStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "development_plans")
@NoArgsConstructor
@SuperBuilder
public class DevelopmentPlan extends VersionedEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "career_goal_id")
    private UUID careerGoalId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private DevelopmentPlanStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

}
