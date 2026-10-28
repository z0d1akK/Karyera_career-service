package com.zodiakk.careerservice.development.entity;

import com.zodiakk.careerservice.common.entity.VersionedIdentifiedEntity;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityStatus;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "development_activities")
@NoArgsConstructor
@SuperBuilder
public class DevelopmentActivity extends VersionedIdentifiedEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "skill_id")
    private UUID skillId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private DevelopmentActivityType activityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private DevelopmentActivityStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

}
