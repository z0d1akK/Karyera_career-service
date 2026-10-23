package com.zodiakk.careerservice.talent.entity;

import com.zodiakk.careerservice.common.entity.IdentifiedEntity;
import com.zodiakk.careerservice.common.enums.ReserveDecisionType;
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
@Table(name = "reserve_decisions")
@NoArgsConstructor
@SuperBuilder
public class ReserveDecision extends IdentifiedEntity {

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "decided_by_employee_id", nullable = false)
    private UUID decidedByEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 25)
    private ReserveDecisionType decision;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "decided_at", nullable = false)
    private OffsetDateTime decidedAt;

}
