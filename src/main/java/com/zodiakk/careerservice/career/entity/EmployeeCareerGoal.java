package com.zodiakk.careerservice.career.entity;

import com.zodiakk.careerservice.common.entity.VersionedCreatedByEntity;
import com.zodiakk.careerservice.common.enums.CareerGoalStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
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
@Table(name = "employee_career_goals")
@NoArgsConstructor
@SuperBuilder
public class EmployeeCareerGoal extends VersionedCreatedByEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "target_position_id", nullable = false)
    private UUID targetPositionId;

    @Column(name = "target_grade_id", nullable = false)
    private UUID targetGradeId;

    @Column(name = "target_node_id")
    private UUID targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CareerGoalStatus status;

    @Column(name = "motivation", columnDefinition = "text")
    private String motivation;

    @Column(name = "target_date")
    private LocalDate targetDate;

}
