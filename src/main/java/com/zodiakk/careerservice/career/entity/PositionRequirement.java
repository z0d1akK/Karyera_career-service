package com.zodiakk.careerservice.career.entity;

import com.zodiakk.careerservice.common.entity.VersionedEntity;
import com.zodiakk.careerservice.common.enums.RequirementStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "position_requirements")
@NoArgsConstructor
@SuperBuilder
public class PositionRequirement extends VersionedEntity {

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequirementStatus status;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "min_experience_months", nullable = false)
    private Integer minExperienceMonths;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

}
