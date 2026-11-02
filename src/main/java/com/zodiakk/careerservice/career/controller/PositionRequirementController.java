package com.zodiakk.careerservice.career.controller;

import com.zodiakk.careerservice.career.dto.request.action.PositionRequirementUpsertRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.PositionRequirementFilterDto;
import com.zodiakk.careerservice.career.dto.response.PositionRequirementResponseDto;
import com.zodiakk.careerservice.career.service.PositionRequirementService;
import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.careerservice.common.dto.response.ValidationErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/career/requirements")
@RequiredArgsConstructor
@Tag(name = "Position requirements", description = "Versioned position and grade requirements")
public class PositionRequirementController {

    private final PositionRequirementService requirementService;

    @GetMapping
    @Operation(summary = "Search position requirements")
    @ApiResponse(responseCode = "200", description = "Requirements successfully retrieved")
    public ResponseEntity<Page<PositionRequirementResponseDto>> list(
            @Valid PositionRequirementFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(requirementService.list(filter, pageRequest));
    }

    @PostMapping
    @Operation(summary = "Create a position requirement draft",
            description = "The server assigns the next revision number")
    @ApiResponse(responseCode = "201", description = "Requirement draft created",
            content = @Content(schema = @Schema(implementation = PositionRequirementResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    public ResponseEntity<PositionRequirementResponseDto> create(
            @Valid @RequestBody PositionRequirementUpsertRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requirementService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a concrete requirement revision")
    @ApiResponse(responseCode = "200", description = "Requirement found",
            content = @Content(schema = @Schema(implementation = PositionRequirementResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Requirement not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<PositionRequirementResponseDto> get(
            @Parameter(description = "Requirement identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(requirementService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Fully update a requirement draft")
    @ApiResponse(responseCode = "200", description = "Requirement updated",
            content = @Content(schema = @Schema(implementation = PositionRequirementResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Requirement is not editable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<PositionRequirementResponseDto> update(
            @Parameter(description = "Requirement identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody PositionRequirementUpsertRequestDto request
    ) {
        return ResponseEntity.ok(requirementService.update(id, request));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a requirement revision")
    @ApiResponse(responseCode = "200", description = "Requirement published",
            content = @Content(schema = @Schema(implementation = PositionRequirementResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Requirement cannot be published",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<PositionRequirementResponseDto> publish(
            @Parameter(description = "Requirement identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(requirementService.publish(id));
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current published requirement for a position and grade")
    @ApiResponse(responseCode = "200", description = "Current requirement found",
            content = @Content(schema = @Schema(implementation = PositionRequirementResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Current requirement not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<PositionRequirementResponseDto> current(
            @Parameter(description = "Position identifier", required = true) @RequestParam UUID positionId,
            @Parameter(description = "Grade identifier", required = true) @RequestParam UUID gradeId
    ) {
        return ResponseEntity.ok(requirementService.current(positionId, gradeId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get the requirement revision history for a position and grade")
    @ApiResponse(responseCode = "200", description = "History retrieved")
    public ResponseEntity<List<PositionRequirementResponseDto>> history(
            @Parameter(description = "Position identifier", required = true) @RequestParam UUID positionId,
            @Parameter(description = "Grade identifier", required = true) @RequestParam UUID gradeId
    ) {
        return ResponseEntity.ok(requirementService.history(positionId, gradeId));
    }
}
