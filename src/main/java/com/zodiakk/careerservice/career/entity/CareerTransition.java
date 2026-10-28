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
@Table(name = "career_transitions")
@NoArgsConstructor
@SuperBuilder
public class CareerTransition extends IdentifiedEntity {

    @Column(name = "source_node_id", nullable = false)
    private UUID sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private UUID targetNodeId;

    @Column(name = "transition_type", nullable = false, length = 30)
    private String transitionType;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "estimated_months")
    private Integer estimatedMonths;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

}
