package com.zodiakk.careerservice.career.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTrackNodeFilterDto {

    @Schema(description = "Track id")
    private UUID trackId;

    @Schema(description = "Position id")
    private UUID positionId;

    @Schema(description = "Grade id")
    private UUID gradeId;

    @Schema(description = "Is entry point")
    private Boolean isEntryPoint;
}
