package com.zodiakk.careerservice.career.dto.request.filter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredSkillFilterDto {

    @Schema(description = "Requirement id")
    private UUID requirementId;

    @Schema(description = "Skill id")
    private UUID skillId;

    @Schema(description = "Mandatory")
    private Boolean mandatory;
}
