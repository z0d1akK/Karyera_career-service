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
public class CareerTrackNodeCreateRequestDto {

    @Schema(description = "Track id")
    @NotNull(message = ValidationMessages.TRACK_ID_REQUIRED)
    private UUID trackId;

    @Schema(description = "Position id")
    @NotNull(message = ValidationMessages.POSITION_ID_REQUIRED)
    private UUID positionId;

    @Schema(description = "Grade id")
    @NotNull(message = ValidationMessages.GRADE_ID_REQUIRED)
    private UUID gradeId;

    @Schema(description = "Label")
    @Size(max = 150, message = ValidationMessages.LABEL_SIZE)
    private String label;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Display order")
    @NotNull(message = ValidationMessages.DISPLAY_ORDER_REQUIRED)
    private Integer displayOrder;

    @Schema(description = "Is entry point")
    private Boolean isEntryPoint;
}
