package com.zodiakk.careerservice.internal.controller;

import com.zodiakk.careerservice.career.dto.request.action.CareerTrackNodeCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTrackPublicationRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.PositionRequirementUpsertRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.RequiredSkillItemDto;
import com.zodiakk.careerservice.career.dto.request.create.CareerTrackCreateRequestDto;
import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.enums.CareerTrackType;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.talent.dto.request.create.TalentReserveCreateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal career API")
class InternalCareerControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("covers every internal career endpoint")
    void shouldCoverEveryInternalEndpoint() throws Exception {
        ReferenceLookupItemDto position = registerReference("Backend Engineer");
        ReferenceLookupItemDto grade = registerReference("Middle");
        ReferenceLookupItemDto skill = registerReference("Java");
        UUID employeeId = registerEmployee(UUID.randomUUID()).id();

        UUID requirementId = publishRequirement(position.id(), grade.id(), skill.id());
        UUID trackId = publishTrack(position.id(), grade.id());
        UUID reserveId = createReserve(position.id(), grade.id());

        mockMvc.perform(asInternal(get("/internal/v1/career/requirements/current"))
                        .param("positionId", position.id().toString())
                        .param("gradeId", grade.id().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(asInternal(get("/internal/v1/career/requirements/{requirementId}", requirementId)))
                .andExpect(status().isOk());

        mockMvc.perform(asInternal(get("/internal/v1/career/tracks/{trackId}/available-targets", trackId))
                        .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(asInternal(get("/internal/v1/career/talent-reserves/{reserveId}/context", reserveId)))
                .andExpect(status().isOk());
    }

    private UUID publishRequirement(UUID positionId, UUID gradeId, UUID skillId) throws Exception {
        JsonNode created = read(mockMvc.perform(asHr(post("/api/v1/career/requirements"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(PositionRequirementUpsertRequestDto.builder()
                                .positionId(positionId)
                                .gradeId(gradeId)
                                .description("Internal requirement")
                                .minExperienceMonths(12)
                                .skills(List.of(RequiredSkillItemDto.builder()
                                        .skillId(skillId)
                                        .requiredLevel(2)
                                        .weight(new BigDecimal("1.00"))
                                        .mandatory(true)
                                        .description("Java")
                                        .build()))
                                .build())))
                .andExpect(status().isCreated()));

        mockMvc.perform(asHr(post("/api/v1/career/requirements/{id}/publish", id(created))))
                .andExpect(status().isOk());

        return id(created);
    }

    private UUID publishTrack(UUID positionId, UUID gradeId) throws Exception {
        JsonNode created = read(mockMvc.perform(asHr(post("/api/v1/career/tracks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackCreateRequestDto.builder()
                                .code("INT")
                                .name("Internal track")
                                .description("Track used by analytics")
                                .type(CareerTrackType.EXPERT)
                                .build())))
                .andExpect(status().isCreated()));
        UUID trackId = id(created);

        mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/nodes", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackNodeCreateRequestDto.builder()
                                .positionId(positionId)
                                .gradeId(gradeId)
                                .label("Middle")
                                .description("Entry node")
                                .displayOrder(0)
                                .isEntryPoint(true)
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/publish", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackPublicationRequestDto.builder()
                                .comment("Publish")
                                .build())))
                .andExpect(status().isOk());

        return trackId;
    }

    private UUID createReserve(UUID positionId, UUID gradeId) throws Exception {
        JsonNode created = read(mockMvc.perform(asHr(post("/api/v1/career/talent-reserves"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(TalentReserveCreateRequestDto.builder()
                                .name("Internal reserve")
                                .description("Context for analytics")
                                .targetPositionId(positionId)
                                .targetGradeId(gradeId)
                                .effectiveFrom(LocalDate.of(2026, 1, 1))
                                .effectiveTo(LocalDate.of(2026, 12, 31))
                                .build())))
                .andExpect(status().isCreated()));

        return id(created);
    }

    private JsonNode read(ResultActions actions) throws Exception {
        MvcResult result = actions.andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static UUID id(JsonNode node) {
        return UUID.fromString(node.get("id").asString());
    }
}
