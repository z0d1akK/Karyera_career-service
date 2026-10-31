package com.zodiakk.careerservice.development.dto.request.action;

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
public class ActivityEvidenceCreateRequestDto {

    @Schema(description = "Evidence type", example = "EXTERNAL_URL")
    @NotBlank(message = ValidationMessages.EVIDENCE_TYPE_REQUIRED)
    @Size(max = 30, message = ValidationMessages.EVIDENCE_TYPE_SIZE)
    private String evidenceType;

    @Schema(description = "Identifier of a related external entity")
    private UUID externalEntityId;

    @Schema(description = "External evidence URL")
    @Size(max = 2048, message = ValidationMessages.EXTERNAL_URL_SIZE)
    private String externalUrl;

    @Schema(description = "Evidence description")
    @Size(max = 5000, message = ValidationMessages.DESCRIPTION_SIZE)
    private String description;
}
