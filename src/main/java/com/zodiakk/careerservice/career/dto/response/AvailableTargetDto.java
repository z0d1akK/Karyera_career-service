package com.zodiakk.careerservice.career.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableTargetDto {

    @Schema(description = "Target node id")
    private UUID nodeId;

    @Schema(description = "Target position id")
    private UUID positionId;

    @Schema(description = "Target grade id")
    private UUID gradeId;

    @Schema(description = "Node label")
    private String label;

    @Schema(description = "Transition type")
    private String transitionType;

    @Schema(description = "Estimated months")
    private Integer estimatedMonths;
}
