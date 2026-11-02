package com.zodiakk.careerservice.career.controller;

import com.zodiakk.careerservice.career.dto.request.action.CareerGoalCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerGoalTransitionRequestDto;
import com.zodiakk.careerservice.career.dto.request.filter.EmployeeCareerGoalFilterDto;
import com.zodiakk.careerservice.career.dto.request.update.EmployeeCareerGoalUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.EmployeeCareerGoalResponseDto;
import com.zodiakk.careerservice.career.service.CareerGoalService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/career")
@RequiredArgsConstructor
@Tag(name = "Career goals", description = "Employee career goals and their lifecycle")
public class CareerGoalController {

    private final CareerGoalService careerGoalService;

    @GetMapping("/employees/{employeeId}/goals")
    @Operation(summary = "Get the career goal history of an employee")
    @ApiResponse(responseCode = "200", description = "Goals retrieved")
    @ApiResponse(responseCode = "403", description = "No access to the employee",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Page<EmployeeCareerGoalResponseDto>> list(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId,
            @Valid EmployeeCareerGoalFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(careerGoalService.listByEmployee(employeeId, filter, pageRequest));
    }

    @PostMapping("/employees/{employeeId}/goals")
    @Operation(summary = "Create a career goal for an employee")
    @ApiResponse(responseCode = "201", description = "Career goal created",
            content = @Content(schema = @Schema(implementation = EmployeeCareerGoalResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Goal cannot be created",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<EmployeeCareerGoalResponseDto> create(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId,
            @Valid @RequestBody CareerGoalCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careerGoalService.create(employeeId, request));
    }

    @GetMapping("/goals/{id}")
    @Operation(summary = "Get a career goal")
    @ApiResponse(responseCode = "200", description = "Career goal found",
            content = @Content(schema = @Schema(implementation = EmployeeCareerGoalResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Career goal not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<EmployeeCareerGoalResponseDto> get(
            @Parameter(description = "Career goal identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(careerGoalService.get(id));
    }

    @PatchMapping("/goals/{id}")
    @Operation(summary = "Update a career goal")
    @ApiResponse(responseCode = "200", description = "Career goal updated",
            content = @Content(schema = @Schema(implementation = EmployeeCareerGoalResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Career goal cannot be updated",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<EmployeeCareerGoalResponseDto> update(
            @Parameter(description = "Career goal identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody EmployeeCareerGoalUpdateRequestDto request
    ) {
        return ResponseEntity.ok(careerGoalService.update(id, request));
    }

    @PostMapping("/goals/{id}/activate")
    @Operation(summary = "Activate a career goal")
    @ApiResponse(responseCode = "200", description = "Career goal activated",
            content = @Content(schema = @Schema(implementation = EmployeeCareerGoalResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Career goal cannot be activated",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<EmployeeCareerGoalResponseDto> activate(
            @Parameter(description = "Career goal identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) CareerGoalTransitionRequestDto request
    ) {
        return ResponseEntity.ok(careerGoalService.activate(id, request));
    }

    @PostMapping("/goals/{id}/achieve")
    @Operation(summary = "Mark a career goal as achieved")
    @ApiResponse(responseCode = "200", description = "Career goal achieved",
            content = @Content(schema = @Schema(implementation = EmployeeCareerGoalResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Career goal cannot be achieved",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<EmployeeCareerGoalResponseDto> achieve(
            @Parameter(description = "Career goal identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) CareerGoalTransitionRequestDto request
    ) {
        return ResponseEntity.ok(careerGoalService.achieve(id, request));
    }

    @PostMapping("/goals/{id}/cancel")
    @Operation(summary = "Cancel a career goal")
    @ApiResponse(responseCode = "200", description = "Career goal cancelled",
            content = @Content(schema = @Schema(implementation = EmployeeCareerGoalResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Career goal cannot be cancelled",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<EmployeeCareerGoalResponseDto> cancel(
            @Parameter(description = "Career goal identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody(required = false) CareerGoalTransitionRequestDto request
    ) {
        return ResponseEntity.ok(careerGoalService.cancel(id, request));
    }
}
