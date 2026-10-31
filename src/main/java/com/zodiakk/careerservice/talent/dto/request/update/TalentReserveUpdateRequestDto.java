package com.zodiakk.careerservice.talent.dto.request.update;

import com.zodiakk.careerservice.common.enums.*;
import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentReserveUpdateRequestDto {

    @Schema(description = "Name")
    @Size(max = 200, message = ValidationMessages.NAME_SIZE)
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Target position id")
    private UUID targetPositionId;

    @Schema(description = "Target grade id")
    private UUID targetGradeId;

    @Schema(description = "Status")
    private TalentReserveStatus status;

    @Schema(description = "Effective from")
    private LocalDate effectiveFrom;

    @Schema(description = "Effective to")
    private LocalDate effectiveTo;
}
