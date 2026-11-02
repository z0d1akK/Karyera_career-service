package com.zodiakk.careerservice.development.controller;

import com.zodiakk.careerservice.common.dto.PageRequestDto;
import com.zodiakk.careerservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.careerservice.common.dto.response.ValidationErrorResponse;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivityCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanCompletionRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanReviewRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanSubmitRequestDto;
import com.zodiakk.careerservice.development.dto.request.filter.DevelopmentPlanFilterDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentPlanUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentActivityResponseDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentPlanResponseDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentProgressResponseDto;
import com.zodiakk.careerservice.development.dto.response.PlanReviewResponseDto;
import com.zodiakk.careerservice.development.service.DevelopmentPlanService;

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
@Tag(name = "Development plans", description = "Individual development plans, activities and progress")
public class DevelopmentPlanController {

    private final DevelopmentPlanService developmentPlanService;

    @GetMapping("/employees/{employeeId}/development-plans")
    @Operation(summary = "Get the development plans of an employee")
    @ApiResponse(responseCode = "200", description = "Plans retrieved")
    @ApiResponse(responseCode = "403", description = "No access to the employee",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Page<DevelopmentPlanResponseDto>> list(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId,
            @Valid DevelopmentPlanFilterDto filter,
            @Valid PageRequestDto pageRequest
    ) {
        return ResponseEntity.ok(developmentPlanService.listByEmployee(employeeId, filter, pageRequest));
    }

    @GetMapping("/employees/{employeeId}/development-progress")
    @Operation(summary = "Get the professional development progress of an employee",
            description = "Calculates progress based on the completion of development activities")
    @ApiResponse(responseCode = "200", description = "Progress retrieved",
            content = @Content(schema = @Schema(implementation = DevelopmentProgressResponseDto.class)))
    @ApiResponse(responseCode = "403", description = "No access to the employee",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentProgressResponseDto> progress(
            @Parameter(description = "Employee identifier", required = true) @PathVariable UUID employeeId
    ) {
        return ResponseEntity.ok(developmentPlanService.progress(employeeId));
    }

    @PostMapping("/development-plans")
    @Operation(summary = "Create a development plan")
    @ApiResponse(responseCode = "201", description = "Development plan created",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> create(
            @Valid @RequestBody DevelopmentPlanCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(developmentPlanService.create(request));
    }

    @GetMapping("/development-plans/{id}")
    @Operation(summary = "Get a development plan")
    @ApiResponse(responseCode = "200", description = "Development plan found",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Development plan not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> get(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(developmentPlanService.get(id));
    }

    @PatchMapping("/development-plans/{id}")
    @Operation(summary = "Update a development plan draft")
    @ApiResponse(responseCode = "200", description = "Development plan updated",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Development plan is not editable",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> update(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentPlanUpdateRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.update(id, request));
    }

    @PostMapping("/development-plans/{id}/submit")
    @Operation(summary = "Submit a development plan for approval")
    @ApiResponse(responseCode = "200", description = "Development plan submitted",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Development plan cannot be submitted",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> submit(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentPlanSubmitRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.submit(id, request));
    }

    @PostMapping("/development-plans/{id}/approve")
    @Operation(summary = "Approve a development plan")
    @ApiResponse(responseCode = "200", description = "Development plan approved",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "403", description = "Not allowed to approve the plan",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Development plan cannot be approved",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> approve(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentPlanReviewRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.approve(id, request));
    }

    @PostMapping("/development-plans/{id}/request-changes")
    @Operation(summary = "Return a development plan for revision")
    @ApiResponse(responseCode = "200", description = "Changes requested",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Changes cannot be requested",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> requestChanges(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentPlanReviewRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.requestChanges(id, request));
    }

    @PostMapping("/development-plans/{id}/complete")
    @Operation(summary = "Confirm the completion of a development plan")
    @ApiResponse(responseCode = "200", description = "Development plan completed",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Development plan cannot be completed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> complete(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentPlanCompletionRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.complete(id, request));
    }

    @PostMapping("/development-plans/{id}/cancel")
    @Operation(summary = "Cancel a development plan")
    @ApiResponse(responseCode = "200", description = "Development plan cancelled",
            content = @Content(schema = @Schema(implementation = DevelopmentPlanResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Development plan cannot be cancelled",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentPlanResponseDto> cancel(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentPlanCompletionRequestDto request
    ) {
        return ResponseEntity.ok(developmentPlanService.cancel(id, request));
    }

    @GetMapping("/development-plans/{id}/reviews")
    @Operation(summary = "Get the approval history of a development plan")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved")
    public ResponseEntity<List<PlanReviewResponseDto>> reviews(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id
    ) {
        return ResponseEntity.ok(developmentPlanService.reviews(id));
    }

    @PostMapping("/development-plans/{id}/activities")
    @Operation(summary = "Add an activity to a development plan")
    @ApiResponse(responseCode = "201", description = "Activity added",
            content = @Content(schema = @Schema(implementation = DevelopmentActivityResponseDto.class)))
    @ApiResponse(responseCode = "409", description = "Activity cannot be added",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DevelopmentActivityResponseDto> addActivity(
            @Parameter(description = "Development plan identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody DevelopmentActivityCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(developmentPlanService.addActivity(id, request));
    }
}
