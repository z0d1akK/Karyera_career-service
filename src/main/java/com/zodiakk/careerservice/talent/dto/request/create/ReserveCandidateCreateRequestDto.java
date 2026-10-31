package com.zodiakk.careerservice.talent.dto.request.create;

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
public class ReserveCandidateCreateRequestDto {

    @Schema(description = "Reserve id")
    @NotNull(message = ValidationMessages.RESERVE_ID_REQUIRED)
    private UUID reserveId;

    @Schema(description = "Employee id")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Assessment id")
    private UUID assessmentId;

    @Schema(description = "Comment")
    private String comment;
}
