package com.zodiakk.careerservice.development.controller;

import com.zodiakk.careerservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.careerservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.careerservice.development.dto.request.action.ActivityEvidenceCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivitySubmitRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivityVerificationRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentActivityUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.ActivityEvidenceResponseDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentActivityResponseDto;
import com.zodiakk.careerservice.development.service.DevelopmentPlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/career/development-activities")
@RequiredArgsConstructor
@Tag(name = "Development activities", description = "Activities of individual development plans")
public class DevelopmentActivityController {

    private final DevelopmentPlanService developmentPlanService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update a development activity")
    @ApiResponse(responseCode = "200", description = "Activity updated",
            content = @Content(schema = @Schema(implementation = DevelopmentActivityResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Activity is not editable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentActivityResponseDto> update(
            @Parameter(description = "Activity identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentActivityUpdateRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.updateActivity(id, request));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Report the completion of a development activity")
    @ApiResponse(responseCode = "200", description = "Activity submitted for verification",
            content = @Content(schema = @Schema(implementation = DevelopmentActivityResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Activity cannot be submitted",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentActivityResponseDto> submit(
            @Parameter(description = "Activity identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentActivitySubmitRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.submitActivity(id, request));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Confirm the completion of a development activity")
    @ApiResponse(responseCode = "200", description = "Activity verified",
            content = @Content(schema = @Schema(implementation = DevelopmentActivityResponseDto.class)))
    @ApiResponse(responseCode = "403", description = "Not allowed to verify the activity",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Activity cannot be verified",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentActivityResponseDto> verify(
            @Parameter(description = "Activity identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentActivityVerificationRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.verifyActivity(id, request));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject the reported completion of a development activity")
    @ApiResponse(responseCode = "200", description = "Activity rejected",
            content = @Content(schema = @Schema(implementation = DevelopmentActivityResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Activity cannot be rejected",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentActivityResponseDto> reject(
            @Parameter(description = "Activity identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentActivityVerificationRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.rejectActivity(id, request));
    }

    @PostMapping("/{id}/evidence")
    @Operation(summary = "Add evidence to a development activity")
    @ApiResponse(responseCode = "201", description = "Evidence added",
            content = @Content(schema = @Schema(implementation = ActivityEvidenceResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Evidence cannot be added",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ActivityEvidenceResponseDto> addEvidence(
            @Parameter(description = "Activity identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ActivityEvidenceCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(developmentPlanService.addEvidence(id, request));
    }
}
