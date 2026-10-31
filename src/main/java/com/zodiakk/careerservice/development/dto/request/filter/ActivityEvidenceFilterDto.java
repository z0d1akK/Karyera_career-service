package com.zodiakk.careerservice.development.dto.request.filter;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityEvidenceFilterDto {

    @Schema(description = "Activity id")
    private UUID activityId;

    @Schema(description = "Verification status")
    private EvidenceVerificationStatus verificationStatus;
}
