package com.zodiakk.careerservice.talent.dto.request.create;

import com.zodiakk.careerservice.common.enums.*;
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
public class ReserveDecisionCreateRequestDto {

    @Schema(description = "Candidate id")
    @NotNull(message = ValidationMessages.CANDIDATE_ID_REQUIRED)
    private UUID candidateId;

    @Schema(description = "Decision")
    @NotNull(message = ValidationMessages.DECISION_REQUIRED)
    private ReserveDecisionType decision;

    @Schema(description = "Comment")
    private String comment;

    @Schema(description = "Assessment id")
    private UUID assessmentId;
}
