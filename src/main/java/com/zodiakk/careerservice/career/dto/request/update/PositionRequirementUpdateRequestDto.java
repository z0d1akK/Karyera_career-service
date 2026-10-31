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
public class PositionRequirementUpdateRequestDto {

    @Schema(description = "Status")
    private RequirementStatus status;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Min experience months")
    private Integer minExperienceMonths;

    @Schema(description = "Published at")
    private OffsetDateTime publishedAt;

    @Schema(description = "Published by")
    private UUID publishedBy;
}
