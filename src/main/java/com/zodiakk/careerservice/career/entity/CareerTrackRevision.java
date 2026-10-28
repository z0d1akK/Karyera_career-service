package com.zodiakk.careerservice.career.entity;

import com.zodiakk.careerservice.common.entity.CreatedEntity;
import com.zodiakk.careerservice.common.enums.CareerRevisionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "career_track_revisions")
@NoArgsConstructor
@SuperBuilder
public class CareerTrackRevision extends CreatedEntity {

    @Column(name = "track_id", nullable = false)
    private UUID trackId;

    @Column(name = "revision", nullable = false)
    private Integer revision;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CareerRevisionStatus status;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

}
