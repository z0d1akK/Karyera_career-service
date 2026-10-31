package com.zodiakk.careerservice.career.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredSkillUpdateRequestDto {

    @Schema(description = "Required level")
    private Integer requiredLevel;

    @Schema(description = "Weight")
    private BigDecimal weight;

    @Schema(description = "Mandatory")
    private Boolean mandatory;

    @Schema(description = "Description")
    private String description;
}
