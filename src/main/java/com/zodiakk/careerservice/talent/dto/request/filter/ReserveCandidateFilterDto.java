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
public class ReserveCandidateFilterDto {

    @Schema(description = "Reserve id")
    private UUID reserveId;

    @Schema(description = "Employee id")
    private UUID employeeId;

    @Schema(description = "Status")
    private ReserveCandidateStatus status;
}
