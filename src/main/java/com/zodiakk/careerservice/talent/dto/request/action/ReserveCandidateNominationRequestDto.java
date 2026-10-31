package com.zodiakk.careerservice.talent.dto.request.action;

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
public class ReserveCandidateNominationRequestDto {

    @Schema(description = "Employee identifier")
    @NotNull(message = ValidationMessages.EMPLOYEE_ID_REQUIRED)
    private UUID employeeId;

    @Schema(description = "Assessment identifier supporting the nomination")
    private UUID assessmentId;

    @Schema(description = "Nomination comment")
    @Size(max = 5000, message = ValidationMessages.COMMENT_SIZE)
    private String comment;
}
