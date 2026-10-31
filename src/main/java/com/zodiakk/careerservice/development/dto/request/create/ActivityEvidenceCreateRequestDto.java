package com.zodiakk.careerservice.development.dto.request.create;

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

    @Schema(description = "Activity id")
    @NotNull(message = ValidationMessages.ACTIVITY_ID_REQUIRED)
    private UUID activityId;

    @Schema(description = "Evidence type")
    @NotBlank(message = ValidationMessages.EVIDENCE_TYPE_REQUIRED)
    @Size(max = 30, message = ValidationMessages.EVIDENCE_TYPE_SIZE)
    private String evidenceType;

    @Schema(description = "External entity id")
    private UUID externalEntityId;

    @Schema(description = "External url")
    @Size(max = 2048, message = ValidationMessages.EXTERNAL_URL_SIZE)
    private String externalUrl;

    @Schema(description = "Description")
    private String description;
}
