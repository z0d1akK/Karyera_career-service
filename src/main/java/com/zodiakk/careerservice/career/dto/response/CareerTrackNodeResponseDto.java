package com.zodiakk.careerservice.career.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTrackNodeResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Track id")
    private UUID trackId;

    @Schema(description = "Position id")
    private UUID positionId;

    @Schema(description = "Grade id")
    private UUID gradeId;

    @Schema(description = "Label")
    private String label;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Is entry point")
    private Boolean isEntryPoint;

    @Schema(description = "Transitions starting from this node")
    private List<CareerTransitionResponseDto> outgoingTransitions;
}
