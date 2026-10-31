package com.zodiakk.careerservice.career.dto.request.filter;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTrackRevisionFilterDto {

    @Schema(description = "Track id")
    private UUID trackId;

    @Schema(description = "Status")
    private CareerRevisionStatus status;
}
