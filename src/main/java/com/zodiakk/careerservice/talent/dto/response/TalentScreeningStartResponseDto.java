package com.zodiakk.careerservice.talent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TalentScreeningStartResponseDto {

    @Schema(description = "Reserve id")
    private UUID reserveId;

    @Schema(description = "Analytics screening job id")
    private UUID jobId;

    @Schema(description = "Screening job status")
    private String status;
}
