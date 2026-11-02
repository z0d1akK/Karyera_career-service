package com.zodiakk.careerservice.career.controller;

import com.zodiakk.careerservice.career.dto.request.action.CareerTrackNodeCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTrackPublicationRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTransitionCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.create.CareerTrackCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.CareerTrackFilterDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackNodeUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.AvailableCareerTrackDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackNodeResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackResponseDto;
import com.zodiakk.careerservice.career.dto.response.CareerTransitionResponseDto;
import com.zodiakk.careerservice.career.service.CareerTrackService;
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
@RequestMapping("/api/v1/career")
@RequiredArgsConstructor
@Tag(name = "Career tracks", description = "Career track structure and publishing")
public class CareerTrackController {

    private final CareerTrackService careerTrackService;

    @GetMapping("/tracks")
    @Operation(summary = "Search career tracks", description = "Returns a paginated list of career tracks")
    @ApiResponse(responseCode = "200", description = "Tracks successfully retrieved")
    public ResponseEntity<Page<CareerTrackResponseDto>> list(
            @Valid CareerTrackFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(careerTrackService.list(filter, pageRequest));
    }

    @PostMapping("/tracks")
    @Operation(summary = "Create a career track draft")
    @ApiResponse(responseCode = "201", description = "Career track draft created",
            content = @Content(schema = @Schema(implementation = CareerTrackResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Career track with the specified code already exists",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackResponseDto> create(
            @Valid @RequestBody CareerTrackCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(careerTrackService.create(request));
    }

    @GetMapping("/tracks/{id}")
    @Operation(summary = "Get a career track", description = "Returns a career track with its nodes and transitions")
    @ApiResponse(responseCode = "200", description = "Career track found",
            content = @Content(schema = @Schema(implementation = CareerTrackResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Career track not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackResponseDto> get(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(careerTrackService.get(id));
    }

    @PatchMapping("/tracks/{id}")
    @Operation(summary = "Update a career track draft")
    @ApiResponse(responseCode = "200", description = "Career track updated",
            content = @Content(schema = @Schema(implementation = CareerTrackResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Career track not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Career track is not editable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackResponseDto> update(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody CareerTrackUpdateRequestDto request
    ) {
        return ResponseEntity.ok(careerTrackService.update(id, request));
    }

    @PostMapping("/tracks/{id}/nodes")
    @Operation(summary = "Add a node to a career track draft")
    @ApiResponse(responseCode = "201", description = "Node added",
            content = @Content(schema = @Schema(implementation = CareerTrackNodeResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Career track not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Node already exists or track is not editable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackNodeResponseDto> addNode(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody CareerTrackNodeCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careerTrackService.addNode(id, request));
    }

    @PatchMapping("/nodes/{id}")
    @Operation(summary = "Update a career track node")
    @ApiResponse(responseCode = "200", description = "Node updated",
            content = @Content(schema = @Schema(implementation = CareerTrackNodeResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Node not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackNodeResponseDto> updateNode(
            @Parameter(description = "Node identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody CareerTrackNodeUpdateRequestDto request
    ) {
        return ResponseEntity.ok(careerTrackService.updateNode(id, request));
    }

    @DeleteMapping("/nodes/{id}")
    @Operation(summary = "Delete a career track node from the draft")
    @ApiResponse(responseCode = "204", description = "Node deleted")
    @ApiResponse(responseCode = "404", description = "Node not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> deleteNode(
            @Parameter(description = "Node identifier", required = true) @PathVariable UUID id
    ) {
        careerTrackService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tracks/{id}/transitions")
    @Operation(summary = "Add a transition to a career track draft")
    @ApiResponse(responseCode = "201", description = "Transition added",
            content = @Content(schema = @Schema(implementation = CareerTransitionResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Transition conflicts with the track structure",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTransitionResponseDto> addTransition(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody CareerTransitionCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careerTrackService.addTransition(id, request));
    }

    @DeleteMapping("/transitions/{id}")
    @Operation(summary = "Delete a career track transition")
    @ApiResponse(responseCode = "204", description = "Transition deleted")
    @ApiResponse(responseCode = "404", description = "Transition not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> deleteTransition(
            @Parameter(description = "Transition identifier", required = true) @PathVariable UUID id
    ) {
        careerTrackService.deleteTransition(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tracks/{id}/publish")
    @Operation(summary = "Publish a career track")
    @ApiResponse(responseCode = "200", description = "Career track published",
            content = @Content(schema = @Schema(implementation = CareerTrackResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Career track cannot be published",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackResponseDto> publish(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) CareerTrackPublicationRequestDto request
    ) {
        return ResponseEntity.ok(careerTrackService.publish(id, request));
    }

    @PostMapping("/tracks/{id}/archive")
    @Operation(summary = "Archive a career track")
    @ApiResponse(responseCode = "200", description = "Career track archived",
            content = @Content(schema = @Schema(implementation = CareerTrackResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Career track cannot be archived",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<CareerTrackResponseDto> archive(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) CareerTrackPublicationRequestDto request
    ) {
        return ResponseEntity.ok(careerTrackService.archive(id, request));
    }

    @GetMapping("/employees/{employeeId}/available-tracks")
    @Operation(summary = "Get career tracks available to an employee")
    @ApiResponse(responseCode = "200", description = "Available tracks retrieved")
    @ApiResponse(responseCode = "403", description = "No access to the employee",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<List<AvailableCareerTrackDto>> availableTracks(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(careerTrackService.availableTracks(employeeId));
    }
}
