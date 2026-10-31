package com.zodiakk.careerservice.career.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTransitionResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Source node id")
    private UUID sourceNodeId;

    @Schema(description = "Target node id")
    private UUID targetNodeId;

    @Schema(description = "Transition type")
    private String transitionType;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Estimated months")
    private Integer estimatedMonths;

    @Schema(description = "Active")
    private Boolean active;
}
