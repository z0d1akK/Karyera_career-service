package com.zodiakk.careerservice.talent.entity;

import com.zodiakk.careerservice.common.entity.VersionedIdentifiedEntity;
import com.zodiakk.careerservice.common.enums.ReserveCandidateStatus;

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
@Table(name = "reserve_candidates")
@NoArgsConstructor
@SuperBuilder
public class ReserveCandidate extends VersionedIdentifiedEntity {

    @Column(name = "reserve_id", nullable = false)
    private UUID reserveId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private ReserveCandidateStatus status;

    @Column(name = "nominated_by", nullable = false)
    private UUID nominatedBy;

    @Column(name = "nominated_at", nullable = false)
    private OffsetDateTime nominatedAt;

    @Column(name = "included_at")
    private OffsetDateTime includedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

}
