package com.zodiakk.careerservice.career.dto.request.update;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTrackUpdateRequestDto {

    @Schema(description = "Code")
    @Size(max = 50, message = ValidationMessages.CODE_SIZE)
    private String code;

    @Schema(description = "Name")
    @Size(max = 150, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Type")
    private CareerTrackType type;

    @Schema(description = "Status")
    private CareerTrackStatus status;
}
