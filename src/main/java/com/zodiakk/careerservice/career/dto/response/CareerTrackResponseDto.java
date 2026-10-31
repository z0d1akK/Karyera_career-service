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
public class CareerTrackResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated at")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version")
    private Long version;

    @Schema(description = "Created by")
    private UUID createdBy;

    @Schema(description = "Code")
    private String code;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Type")
    private CareerTrackType type;

    @Schema(description = "Status")
    private CareerTrackStatus status;

    @Schema(description = "Career track nodes ordered by display order")
    private List<CareerTrackNodeResponseDto> nodes;

    @Schema(description = "Transitions between track nodes")
    private List<CareerTransitionResponseDto> transitions;

    @Schema(description = "Current published revision")
    private CareerTrackRevisionResponseDto currentRevision;
}
