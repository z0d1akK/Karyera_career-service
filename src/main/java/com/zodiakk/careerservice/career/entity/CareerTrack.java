package com.zodiakk.careerservice.career.entity;

import com.zodiakk.careerservice.common.entity.VersionedCreatedByEntity;
import com.zodiakk.careerservice.common.enums.CareerTrackStatus;
import com.zodiakk.careerservice.common.enums.CareerTrackType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "career_tracks")
@NoArgsConstructor
@SuperBuilder
public class CareerTrack extends VersionedCreatedByEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private CareerTrackType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CareerTrackStatus status;

}
