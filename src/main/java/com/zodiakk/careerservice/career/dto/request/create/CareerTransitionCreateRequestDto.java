package com.zodiakk.careerservice.career.dto.request.create;

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

    @Schema(description = "Source node id")
    @NotNull(message = ValidationMessages.SOURCE_NODE_ID_REQUIRED)
    private UUID sourceNodeId;

    @Schema(description = "Target node id")
    @NotNull(message = ValidationMessages.TARGET_NODE_ID_REQUIRED)
    private UUID targetNodeId;

    @Schema(description = "Transition type")
    @NotBlank(message = ValidationMessages.TRANSITION_TYPE_REQUIRED)
    @Size(max = 30, message = ValidationMessages.TRANSITION_TYPE_SIZE)
    private String transitionType;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Estimated months")
    private Integer estimatedMonths;

    @Schema(description = "Active")
    private Boolean active;
}
