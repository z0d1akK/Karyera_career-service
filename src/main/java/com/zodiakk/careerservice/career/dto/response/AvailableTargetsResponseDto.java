package com.zodiakk.careerservice.career.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTargetsResponseDto {

    @Schema(description = "Career track id")
    private UUID trackId;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Node matching the employee's current position and grade")
    private UUID currentNodeId;

    @Schema(description = "Reachable target nodes")
    private List<AvailableTargetDto> targets;
}
