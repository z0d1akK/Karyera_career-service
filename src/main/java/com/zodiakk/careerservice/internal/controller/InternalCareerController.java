package com.zodiakk.careerservice.internal.controller;

import com.zodiakk.careerservice.career.dto.response.AvailableTargetDto;
import com.zodiakk.careerservice.career.dto.response.AvailableTargetsResponseDto;
import com.zodiakk.careerservice.career.dto.response.PositionRequirementResponseDto;
import com.zodiakk.careerservice.career.dto.response.RequiredSkillResponseDto;
import com.zodiakk.careerservice.career.service.CareerTrackService;
import com.zodiakk.careerservice.career.service.PositionRequirementService;
import com.zodiakk.careerservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.careerservice.internal.dto.InternalAvailableTargetsResponseDto;
import com.zodiakk.careerservice.internal.dto.InternalRequirementResponseDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveContextDto;
import com.zodiakk.careerservice.talent.service.TalentReserveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/career")
@RequiredArgsConstructor
@Tag(name = "Internal career API", description = "Internal contract used by Analytics Service")
public class InternalCareerController {

    private final PositionRequirementService requirementService;

    private final CareerTrackService careerTrackService;

    private final TalentReserveService talentReserveService;

    @GetMapping("/requirements/current")
    @Operation(summary = "Get the current published requirement for a position and grade")
    @ApiResponse(responseCode = "200", description = "Current requirement found",
            content = @Content(schema = @Schema(implementation = InternalRequirementResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Current requirement not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<InternalRequirementResponseDto> currentRequirement(
            @Parameter(description = "Position identifier", required = true) @RequestParam UUID positionId,
            @Parameter(description = "Grade identifier", required = true) @RequestParam UUID gradeId
    ) {
        return ResponseEntity.ok(toInternal(requirementService.current(positionId, gradeId)));
    }

    @GetMapping("/requirements/{requirementId}")
    @Operation(summary = "Get a requirement revision by identifier")
    @ApiResponse(responseCode = "200", description = "Requirement found",
            content = @Content(schema = @Schema(implementation = InternalRequirementResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Requirement not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<InternalRequirementResponseDto> requirement(
            @Parameter(description = "Requirement identifier", required = true) @PathVariable UUID requirementId
    ) {
        return ResponseEntity.ok(toInternal(requirementService.get(requirementId)));
    }

    @GetMapping("/tracks/{trackId}/available-targets")
    @Operation(summary = "Get the career targets available to an employee inside a track")
    @ApiResponse(responseCode = "200", description = "Targets retrieved",
            content = @Content(schema = @Schema(implementation = InternalAvailableTargetsResponseDto.class)))
    public ResponseEntity<InternalAvailableTargetsResponseDto> availableTargets(
            @Parameter(description = "Career track identifier", required = true) @PathVariable UUID trackId,
            @Parameter(description = "Employee identifier", required = true) @RequestParam UUID employeeId
    ) {
        AvailableTargetsResponseDto targets = careerTrackService.availableTargets(trackId, employeeId);

        return ResponseEntity.ok(new InternalAvailableTargetsResponseDto(
                targets.getTrackId(),
                targets.getEmployeeId(),
                targets.getCurrentNodeId(),
                targets.getTargets() == null
                        ? List.of()
                        : targets.getTargets().stream().map(this::toInternal).toList()
        ));
    }

    @GetMapping("/talent-reserves/{reserveId}/context")
    @Operation(summary = "Get the parameters of a talent reserve")
    @ApiResponse(responseCode = "200", description = "Reserve context retrieved",
            content = @Content(schema = @Schema(implementation = TalentReserveContextDto.class)))
    @ApiResponse(responseCode = "404", description = "Talent reserve not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TalentReserveContextDto> reserveContext(
            @Parameter(description = "Talent reserve identifier", required = true) @PathVariable UUID reserveId
    ) {
        return ResponseEntity.ok(talentReserveService.context(reserveId));
    }

    private InternalRequirementResponseDto toInternal(PositionRequirementResponseDto response) {
        return new InternalRequirementResponseDto(
                response.getId(),
                response.getPositionId(),
                response.getGradeId(),
                response.getRevision(),
                response.getStatus(),
                response.getMinExperienceMonths(),
                response.getDescription(),
                response.getSkills() == null
                        ? List.of()
                        : response.getSkills().stream().map(this::toInternal).toList()
        );
    }

    private InternalRequirementResponseDto.Skill toInternal(RequiredSkillResponseDto skill) {
        return new InternalRequirementResponseDto.Skill(
                skill.getSkillId(),
                skill.getRequiredLevel(),
                skill.getWeight(),
                skill.getMandatory(),
                skill.getDescription()
        );
    }

    private InternalAvailableTargetsResponseDto.Target toInternal(AvailableTargetDto target) {
        return new InternalAvailableTargetsResponseDto.Target(
                target.getNodeId(),
                target.getPositionId(),
                target.getGradeId(),
                target.getLabel(),
                target.getTransitionType(),
                target.getEstimatedMonths()
        );
    }
}
