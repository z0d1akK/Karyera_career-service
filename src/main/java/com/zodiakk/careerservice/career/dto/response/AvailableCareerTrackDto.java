package com.zodiakk.careerservice.career.dto.response;

import com.zodiakk.careerservice.common.enums.CareerTrackType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableCareerTrackDto {

    @Schema(description = "Career track id")
    private UUID trackId;

    @Schema(description = "Track code")
    private String code;

    @Schema(description = "Track name")
    private String name;

    @Schema(description = "Track type")
    private CareerTrackType type;

    @Schema(description = "Node matching the employee's current position and grade")
    private UUID currentNodeId;

    @Schema(description = "Nodes reachable from the current node")
    private List<UUID> availableTargetNodeIds;
}
