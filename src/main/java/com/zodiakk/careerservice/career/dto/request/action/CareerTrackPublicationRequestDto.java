package com.zodiakk.careerservice.career.dto.request.action;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerTrackPublicationRequestDto {

    @Schema(description = "Optional publication comment")
    @Size(max = 2000, message = ValidationMessages.COMMENT_SIZE)
    private String comment;

    @Schema(description = "Expected entity version for optimistic locking")
    private Long expectedVersion;
}
