package com.zodiakk.careerservice.development.dto.request.update;

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
public class ActivityEvidenceUpdateRequestDto {

    @Schema(description = "Evidence type")
    @Size(max = 30, message = ValidationMessages.EVIDENCE_TYPE_SIZE)
    private String evidenceType;

    @Schema(description = "External entity id")
    private UUID externalEntityId;

    @Schema(description = "External url")
    @Size(max = 2048, message = ValidationMessages.EXTERNAL_URL_SIZE)
    private String externalUrl;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Verification status")
    private EvidenceVerificationStatus verificationStatus;

    @Schema(description = "Verified by")
    private UUID verifiedBy;

    @Schema(description = "Verified at")
    private OffsetDateTime verifiedAt;
}
