package com.zodiakk.careerservice.career.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTransitionFilterDto {

    @Schema(description = "Source node id")
    private UUID sourceNodeId;

    @Schema(description = "Target node id")
    private UUID targetNodeId;

    @Schema(description = "Active")
    private Boolean active;
}
