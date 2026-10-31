package com.zodiakk.careerservice.talent.dto.request.filter;

import com.zodiakk.careerservice.common.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReserveDecisionFilterDto {

    @Schema(description = "Candidate id")
    private UUID candidateId;

    @Schema(description = "Decision")
    private ReserveDecisionType decision;
}
