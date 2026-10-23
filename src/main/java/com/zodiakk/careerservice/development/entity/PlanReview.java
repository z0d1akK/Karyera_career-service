package com.zodiakk.careerservice.development.entity;

import com.zodiakk.careerservice.common.entity.IdentifiedEntity;
import com.zodiakk.careerservice.common.enums.PlanReviewDecision;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "plan_reviews")
@NoArgsConstructor
@SuperBuilder
public class PlanReview extends IdentifiedEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "reviewer_employee_id", nullable = false)
    private UUID reviewerEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 25)
    private PlanReviewDecision decision;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

}
