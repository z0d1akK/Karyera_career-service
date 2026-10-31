package com.zodiakk.careerservice.career.dto.request.action;

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
public class CareerTransitionCreateRequestDto {

    @Schema(description = "Source career track node identifier")
    @NotNull(message = ValidationMessages.SOURCE_NODE_ID_REQUIRED)
    private UUID sourceNodeId;

    @Schema(description = "Target career track node identifier")
    @NotNull(message = ValidationMessages.TARGET_NODE_ID_REQUIRED)
    private UUID targetNodeId;

    @Schema(description = "Transition type", example = "PROMOTION")
    @NotBlank(message = ValidationMessages.TRANSITION_TYPE_REQUIRED)
    @Size(max = 30, message = ValidationMessages.TRANSITION_TYPE_SIZE)
    private String transitionType;

    @Schema(description = "Transition description")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String description;

    @Schema(description = "Estimated number of months for the transition")
    @Min(value = 0, message = ValidationMessages.ESTIMATED_MONTHS_MIN)
    private Integer estimatedMonths;

    @Schema(description = "Whether the transition is active")
    private Boolean active;
}
