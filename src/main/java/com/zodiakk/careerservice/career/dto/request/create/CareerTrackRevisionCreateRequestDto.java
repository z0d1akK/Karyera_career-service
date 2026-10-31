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
public class CareerTrackRevisionCreateRequestDto {

    @Schema(description = "Track id")
    @NotNull(message = ValidationMessages.TRACK_ID_REQUIRED)
    private UUID trackId;

    @Schema(description = "Snapshot")
    @NotBlank(message = ValidationMessages.SNAPSHOT_REQUIRED)
    private String snapshot;
}
