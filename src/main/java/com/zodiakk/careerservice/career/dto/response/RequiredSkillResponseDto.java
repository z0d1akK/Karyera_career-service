package com.zodiakk.careerservice.career.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredSkillResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Requirement id")
    private UUID requirementId;

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Required level")
    private Integer requiredLevel;

    @Schema(description = "Weight")
    private BigDecimal weight;

    @Schema(description = "Mandatory")
    private Boolean mandatory;

    @Schema(description = "Description")
    private String description;
}
