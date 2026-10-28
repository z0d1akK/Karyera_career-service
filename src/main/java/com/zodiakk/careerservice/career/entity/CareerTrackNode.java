package com.zodiakk.careerservice.career.entity;

import com.zodiakk.careerservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "career_track_nodes")
@NoArgsConstructor
@SuperBuilder
public class CareerTrackNode extends IdentifiedEntity {

    @Column(name = "track_id", nullable = false)
    private UUID trackId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "grade_id", nullable = false)
    private UUID gradeId;

    @Column(name = "label", length = 150)
    private String label;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_entry_point", nullable = false)
    private Boolean isEntryPoint = false;

}
