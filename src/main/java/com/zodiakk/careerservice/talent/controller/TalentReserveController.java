package com.zodiakk.careerservice.talent.controller;

import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.careerservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.careerservice.talent.dto.request.action.ReserveCandidateNominationRequestDto;
import com.zodiakk.careerservice.talent.dto.request.action.TalentReserveTransitionRequestDto;
import com.zodiakk.careerservice.talent.dto.request.create.TalentReserveCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.filter.ReserveCandidateFilterDto;
import com.zodiakk.careerservice.talent.dto.request.filter.TalentReserveFilterDto;
import com.zodiakk.careerservice.talent.dto.request.update.TalentReserveUpdateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveCandidateResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveAnalysisDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentScreeningResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentScreeningStartResponseDto;
import com.zodiakk.careerservice.talent.service.TalentReserveService;

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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/career/talent-reserves")
@RequiredArgsConstructor
@Tag(name = "Talent reserves", description = "Talent reserves, candidates and screening facade")
public class TalentReserveController {

    private final TalentReserveService talentReserveService;

    @GetMapping
    @Operation(summary = "Search talent reserves")
    @ApiResponse(responseCode = "200", description = "Reserves retrieved")
    public ResponseEntity<Page<TalentReserveResponseDto>> list(
            @Valid TalentReserveFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(talentReserveService.list(filter, pageRequest));
    }

    @PostMapping
    @Operation(summary = "Create a talent reserve")
    @ApiResponse(responseCode = "201", description = "Talent reserve created",
            content = @Content(schema = @Schema(implementation = TalentReserveResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    public ResponseEntity<TalentReserveResponseDto> create(
            @Valid @RequestBody TalentReserveCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(talentReserveService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a talent reserve")
    @ApiResponse(responseCode = "200", description = "Talent reserve found",
            content = @Content(schema = @Schema(implementation = TalentReserveResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Talent reserve not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentReserveResponseDto> get(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(talentReserveService.get(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update a talent reserve")
    @ApiResponse(responseCode = "200", description = "Talent reserve updated",
            content = @Content(schema = @Schema(implementation = TalentReserveResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Talent reserve is not editable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentReserveResponseDto> update(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody TalentReserveUpdateRequestDto request
    ) {
        return ResponseEntity.ok(talentReserveService.update(id, request));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a talent reserve")
    @ApiResponse(responseCode = "200", description = "Talent reserve activated",
            content = @Content(schema = @Schema(implementation = TalentReserveResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Talent reserve cannot be activated",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentReserveResponseDto> activate(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) TalentReserveTransitionRequestDto request
    ) {
        return ResponseEntity.ok(talentReserveService.activate(id, request));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a talent reserve")
    @ApiResponse(responseCode = "200", description = "Talent reserve closed",
            content = @Content(schema = @Schema(implementation = TalentReserveResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Talent reserve cannot be closed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentReserveResponseDto> close(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) TalentReserveTransitionRequestDto request
    ) {
        return ResponseEntity.ok(talentReserveService.close(id, request));
    }

    @GetMapping("/{id}/candidates")
    @Operation(summary = "Get the candidates of a talent reserve")
    @ApiResponse(responseCode = "200", description = "Candidates retrieved")
    public ResponseEntity<Page<ReserveCandidateResponseDto>> candidates(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id,
            @Valid ReserveCandidateFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(talentReserveService.candidates(id, filter, pageRequest));
    }

    @PostMapping("/{id}/candidates")
    @Operation(summary = "Nominate an employee to a talent reserve")
    @ApiResponse(responseCode = "201", description = "Candidate nominated",
            content = @Content(schema = @Schema(implementation = ReserveCandidateResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Employee is already an active candidate",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ReserveCandidateResponseDto> nominate(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ReserveCandidateNominationRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(talentReserveService.nominate(id, request));
    }

    @PostMapping("/{id}/screenings")
    @Operation(summary = "Start an analytics pre-screening for a talent reserve",
            description = "Screening does not create reserve candidates automatically")
    @ApiResponse(responseCode = "202", description = "Screening started",
            content = @Content(schema = @Schema(implementation = TalentScreeningStartResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Talent reserve is not active",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Analytics service is unavailable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentScreeningStartResponseDto> startScreening(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(talentReserveService.startScreening(id));
    }

    @GetMapping("/{id}/screenings/{jobId}")
    @Operation(summary = "Get the status and results of a pre-screening")
    @ApiResponse(responseCode = "200", description = "Screening retrieved",
            content = @Content(schema = @Schema(implementation = TalentScreeningResponseDto.class)))
    @ApiResponse(responseCode = "503", description = "Analytics service is unavailable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentScreeningResponseDto> getScreening(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id,
            @Parameter(description = "Screening job identifier", required = true) @PathVariable UUID jobId
    ) {
        return ResponseEntity.ok(talentReserveService.getScreening(id, jobId));
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "Analyse the composition of a talent reserve",
            description = "Returns candidate metrics grouped by status, current position and grade")
    @ApiResponse(responseCode = "200", description = "Analysis retrieved",
            content = @Content(schema = @Schema(implementation = TalentReserveAnalysisDto.class)))
    @ApiResponse(responseCode = "404", description = "Talent reserve not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentReserveAnalysisDto> analysis(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(talentReserveService.analysis(id));
    }
}
