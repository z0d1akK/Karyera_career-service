package com.zodiakk.careerservice.career.dto.response;

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
public class CareerTrackRevisionResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Track id")
    private UUID trackId;

    @Schema(description = "Revision")
    private Integer revision;

    @Schema(description = "Status")
    private CareerRevisionStatus status;

    @Schema(description = "Snapshot")
    private String snapshot;

    @Schema(description = "Published at")
    private OffsetDateTime publishedAt;

    @Schema(description = "Published by")
    private UUID publishedBy;
}
