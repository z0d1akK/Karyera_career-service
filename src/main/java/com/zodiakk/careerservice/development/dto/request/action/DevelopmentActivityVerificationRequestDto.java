package com.zodiakk.careerservice.development.dto.request.action;

import com.zodiakk.careerservice.common.validation.ValidationMessages;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevelopmentActivityVerificationRequestDto {

    @Schema(description = "Verification comment")
    @Size(max = 5000, message = ValidationMessages.COMMENT_SIZE)
    private String comment;

    @Schema(description = "Expected entity version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
