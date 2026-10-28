package com.zodiakk.careerservice.development.entity;

import com.zodiakk.careerservice.common.entity.CreatedEntity;
import com.zodiakk.careerservice.common.enums.EvidenceVerificationStatus;

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
@Table(name = "activity_evidence")
@NoArgsConstructor
@SuperBuilder
public class ActivityEvidence extends CreatedEntity {

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "evidence_type", nullable = false, length = 30)
    private String evidenceType;

    @Column(name = "external_entity_id")
    private UUID externalEntityId;

    @Column(name = "external_url", length = 2048)
    private String externalUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private EvidenceVerificationStatus verificationStatus;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

}
