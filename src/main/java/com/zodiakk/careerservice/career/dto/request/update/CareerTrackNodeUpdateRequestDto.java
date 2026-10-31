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
public class CareerTrackNodeUpdateRequestDto {

    @Schema(description = "Position id")
    private UUID positionId;

    @Schema(description = "Grade id")
    private UUID gradeId;

    @Schema(description = "Label")
    @Size(max = 150, message = ValidationMessages.LABEL_SIZE)
    private String label;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Is entry point")
    private Boolean isEntryPoint;
}
