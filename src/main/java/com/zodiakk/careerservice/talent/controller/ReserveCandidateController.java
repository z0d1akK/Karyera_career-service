package com.zodiakk.careerservice.talent.controller;

import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.careerservice.talent.dto.request.action.ReserveCandidateDecisionCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveCandidateResponseDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveDecisionResponseDto;
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
@RequestMapping("/api/v1/career/reserve-candidates")
@RequiredArgsConstructor
@Tag(name = "Reserve candidates", description = "Talent reserve candidate cards and HR decisions")
public class ReserveCandidateController {

    private final TalentReserveService talentReserveService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a reserve candidate card")
    @ApiResponse(responseCode = "200", description = "Candidate found",
            content = @Content(schema = @Schema(implementation = ReserveCandidateResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Candidate not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ReserveCandidateResponseDto> get(
            @Parameter(description = "Candidate identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(talentReserveService.getCandidate(id));
    }

    @PostMapping("/{id}/decisions")
    @Operation(summary = "Record an HR decision for a reserve candidate",
            description = "The decision and the candidate status change are persisted in one transaction")
    @ApiResponse(responseCode = "201", description = "Decision recorded",
            content = @Content(schema = @Schema(implementation = ReserveDecisionResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Decision conflicts with the candidate state",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ReserveDecisionResponseDto> decide(
            @Parameter(description = "Candidate identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ReserveCandidateDecisionCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(talentReserveService.decide(id, request));
    }

    @GetMapping("/{id}/decisions")
    @Operation(summary = "Get the decision history of a reserve candidate")
    @ApiResponse(responseCode = "200", description = "Decision history retrieved")
    @ApiResponse(responseCode = "404", description = "Candidate not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Page<ReserveDecisionResponseDto>> decisions(
            @Parameter(description = "Candidate identifier", required = true) @PathVariable UUID id,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(talentReserveService.decisions(id, pageRequest));
    }
}
