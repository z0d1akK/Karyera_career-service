package com.zodiakk.careerservice.talent.entity;

import com.zodiakk.careerservice.common.entity.VersionedCreatedByEntity;
import com.zodiakk.careerservice.common.enums.TalentReserveStatus;

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
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "talent_reserves")
@NoArgsConstructor
@SuperBuilder
public class TalentReserve extends VersionedCreatedByEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "target_position_id", nullable = false)
    private UUID targetPositionId;

    @Column(name = "target_grade_id", nullable = false)
    private UUID targetGradeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TalentReserveStatus status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

}
