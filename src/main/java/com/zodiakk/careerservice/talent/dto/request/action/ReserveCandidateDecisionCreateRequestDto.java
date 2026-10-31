package com.zodiakk.careerservice.talent.dto.request.action;

import com.zodiakk.careerservice.common.enums.ReserveDecisionType;
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
public class ReserveCandidateDecisionCreateRequestDto {

    @Schema(description = "Reserve candidate decision", example = "APPROVE")
    @NotNull(message = ValidationMessages.DECISION_REQUIRED)
    private ReserveDecisionType decision;

    @Schema(description = "Decision comment")
    @Size(max = 5000, message = ValidationMessages.COMMENT_SIZE)
    private String comment;

    @Schema(description = "Assessment identifier supporting the decision")
    private UUID assessmentId;

    @Schema(description = "Expected candidate version for optimistic locking")
    @NotNull(message = ValidationMessages.EXPECTED_VERSION_REQUIRED)
    private Long expectedVersion;
}
