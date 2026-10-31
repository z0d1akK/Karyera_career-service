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
public class CareerTrackNodeCreateRequestDto {

    @Schema(description = "Position identifier")
    @NotNull(message = ValidationMessages.POSITION_ID_REQUIRED)
    private UUID positionId;

    @Schema(description = "Grade identifier")
    @NotNull(message = ValidationMessages.GRADE_ID_REQUIRED)
    private UUID gradeId;

    @Schema(description = "Node label", example = "Senior Backend Engineer")
    @Size(max = 150, message = ValidationMessages.LABEL_SIZE)
    private String label;

    @Schema(description = "Node description")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String description;

    @Schema(description = "Display order inside the track", example = "3")
    @NotNull(message = ValidationMessages.DISPLAY_ORDER_REQUIRED)
    @Min(value = 0, message = ValidationMessages.DISPLAY_ORDER_MIN)
    private Integer displayOrder;

    @Schema(description = "Whether the node is an entry point of the track")
    private Boolean isEntryPoint;
}
