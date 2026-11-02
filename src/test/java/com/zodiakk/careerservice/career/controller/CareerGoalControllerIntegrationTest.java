package com.zodiakk.careerservice.career.controller;

import com.zodiakk.careerservice.career.dto.request.action.CareerGoalCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerGoalTransitionRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.EmployeeCareerGoalUpdateRequestDto;
import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Career goals")
class CareerGoalControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("covers every career goal endpoint")
    void shouldCoverEveryCareerGoalEndpoint() throws Exception {
        UUID employeeId = registerEmployee(UUID.randomUUID()).id();
        ReferenceLookupItemDto position = registerReference("Architect");
        ReferenceLookupItemDto grade = registerReference("Lead");

        JsonNode created = read(mockMvc.perform(asHr(post("/api/v1/career/employees/{employeeId}/goals", employeeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(goal(position.id(), grade.id(), "Lead the platform"))))
                .andExpect(status().isCreated()));
        UUID goalId = id(created);

        mockMvc.perform(asHr(get("/api/v1/career/employees/{employeeId}/goals", employeeId))
                        .param("employeeId", employeeId.toString())
                        .param("targetPositionId", position.id().toString())
                        .param("targetGradeId", grade.id().toString())
                        .param("status", "PROPOSED")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "targetDate")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(get("/api/v1/career/goals/{id}", goalId)))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(patch("/api/v1/career/goals/{id}", goalId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(EmployeeCareerGoalUpdateRequestDto.builder()
                                .motivation("Lead the platform team")
                                .targetDate(LocalDate.of(2027, 12, 1))
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(post("/api/v1/career/goals/{id}/activate", goalId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerGoalTransitionRequestDto.builder()
                                .comment("Activate the goal")
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(post("/api/v1/career/goals/{id}/achieve", goalId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerGoalTransitionRequestDto.builder()
                                .comment("Goal achieved")
                                .build())))
                .andExpect(status().isOk());

        JsonNode second = read(mockMvc.perform(asHr(post("/api/v1/career/employees/{employeeId}/goals", employeeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(goal(position.id(), grade.id(), "A goal to cancel"))))
                .andExpect(status().isCreated()));

        mockMvc.perform(asHr(post("/api/v1/career/goals/{id}/cancel", id(second)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerGoalTransitionRequestDto.builder()
                                .comment("Cancel the goal")
                                .build())))
                .andExpect(status().isOk());
    }

    private static CareerGoalCreateRequestDto goal(UUID positionId, UUID gradeId, String motivation) {
        return CareerGoalCreateRequestDto.builder()
                .targetPositionId(positionId)
                .targetGradeId(gradeId)
                .targetDate(LocalDate.of(2027, 6, 1))
                .motivation(motivation)
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
