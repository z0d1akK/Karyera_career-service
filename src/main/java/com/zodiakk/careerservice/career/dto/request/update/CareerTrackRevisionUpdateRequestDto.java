package com.zodiakk.careerservice.career.dto.request.update;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTrackRevisionUpdateRequestDto {

    @Schema(description = "Status")
    private CareerRevisionStatus status;

    @Schema(description = "Snapshot")
    private String snapshot;

    @Schema(description = "Published at")
    private OffsetDateTime publishedAt;

    @Schema(description = "Published by")
    private UUID publishedBy;
}
