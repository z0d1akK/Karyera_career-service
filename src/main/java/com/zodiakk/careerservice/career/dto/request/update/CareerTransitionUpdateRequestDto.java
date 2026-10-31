package com.zodiakk.careerservice.career.dto.request.update;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTransitionUpdateRequestDto {

    @Schema(description = "Target node id")
    private UUID targetNodeId;

    @Schema(description = "Transition type")
    @Size(max = 30, message = ValidationMessages.TRANSITION_TYPE_SIZE)
    private String transitionType;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Estimated months")
    private Integer estimatedMonths;

    @Schema(description = "Active")
    private Boolean active;
}
