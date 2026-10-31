package com.zodiakk.careerservice.development.dto.response;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEvidenceResponseDto {

    @Schema(description = "Id")
    private UUID id;

    @Schema(description = "Created at")
    private OffsetDateTime createdAt;

    @Schema(description = "Activity id")
    private UUID activityId;

    @Schema(description = "Evidence type")
    private String evidenceType;

    @Schema(description = "External entity id")
    private UUID externalEntityId;

    @Schema(description = "External url")
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
