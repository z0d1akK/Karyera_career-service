package com.zodiakk.careerservice.career.entity;

import com.zodiakk.careerservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "required_skills")
@NoArgsConstructor
@SuperBuilder
public class RequiredSkill extends IdentifiedEntity {

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "required_level", nullable = false)
    private Integer requiredLevel;

    @Column(name = "weight", nullable = false, precision = 8, scale = 5)
    private BigDecimal weight;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory;

    @Column(name = "description", columnDefinition = "text")
    private String description;

}
