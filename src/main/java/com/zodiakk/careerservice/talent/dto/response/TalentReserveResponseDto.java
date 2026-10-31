package com.zodiakk.careerservice.talent.dto.response;

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
public class TalentReserveResponseDto {

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

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Status")
    private TalentReserveStatus status;

    @Schema(description = "Effective from")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective to")
    private LocalDate effectiveTo;

    @Schema(description = "Number of reserve candidates")
    private Long candidateCount;
}
