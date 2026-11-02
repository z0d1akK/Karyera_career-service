package com.zodiakk.careerservice.career.controller;

import com.zodiakk.careerservice.career.dto.request.action.PositionRequirementUpsertRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.RequiredSkillItemDto;
import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Position requirements")
class PositionRequirementControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("covers every position requirement endpoint")
    void shouldCoverEveryPositionRequirementEndpoint() throws Exception {
        ReferenceLookupItemDto position = registerReference("Backend Engineer");
        ReferenceLookupItemDto grade = registerReference("Senior");
        ReferenceLookupItemDto skill = registerReference("Java");
        ReferenceLookupItemDto updatedSkill = registerReference("Kotlin");

        JsonNode created = read(mockMvc.perform(asHr(post("/api/v1/career/requirements"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(requirement(position.id(), grade.id(), skill.id(), "Draft requirements", 3))))
                .andExpect(status().isCreated()));
        UUID requirementId = id(created);

        mockMvc.perform(asHr(get("/api/v1/career/requirements"))
                        .param("positionId", position.id().toString())
                        .param("gradeId", grade.id().toString())
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "revision")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(get("/api/v1/career/requirements/{id}", requirementId)))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(put("/api/v1/career/requirements/{id}", requirementId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(requirement(
                                position.id(),
                                grade.id(),
                                updatedSkill.id(),
                                "Updated requirements",
                                4
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(post("/api/v1/career/requirements/{id}/publish", requirementId)))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(get("/api/v1/career/requirements/current"))
                        .param("positionId", position.id().toString())
                        .param("gradeId", grade.id().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(get("/api/v1/career/requirements/history"))
                        .param("positionId", position.id().toString())
                        .param("gradeId", grade.id().toString()))
                .andExpect(status().isOk());
    }

    private static PositionRequirementUpsertRequestDto requirement(
            UUID positionId,
            UUID gradeId,
            UUID skillId,
            String description,
            int level
    ) {
        return PositionRequirementUpsertRequestDto.builder()
                .positionId(positionId)
                .gradeId(gradeId)
                .description(description)
                .minExperienceMonths(24)
                .skills(List.of(RequiredSkillItemDto.builder()
                        .skillId(skillId)
                        .requiredLevel(level)
                        .weight(new BigDecimal("1.50"))
                        .mandatory(true)
                        .description("Java skill")
                        .build()))
                .build();
    }

    private JsonNode read(ResultActions actions) throws Exception {
        MvcResult result = actions.andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static UUID id(JsonNode node) {
        return UUID.fromString(node.get("id").asString());
    }
}
