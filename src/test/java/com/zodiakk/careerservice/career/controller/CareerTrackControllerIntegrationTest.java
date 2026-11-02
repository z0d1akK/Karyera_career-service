package com.zodiakk.careerservice.career.controller;

import com.zodiakk.careerservice.career.dto.request.action.CareerTrackNodeCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTrackPublicationRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.CareerTransitionCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.create.CareerTrackCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackNodeUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackUpdateRequestDto;
import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.enums.CareerTrackType;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Career tracks")
class CareerTrackControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("covers every career track endpoint")
    void shouldCoverEveryCareerTrackEndpoint() throws Exception {
        ReferenceLookupItemDto junior = registerReference("Junior Engineer");
        ReferenceLookupItemDto middle = registerReference("Middle Engineer");
        ReferenceLookupItemDto extra = registerReference("Staff Engineer");
        ReferenceLookupItemDto grade = registerReference("L2");
        UUID employeeId = registerEmployee(UUID.randomUUID()).id();

        JsonNode created = read(mockMvc.perform(asHr(post("/api/v1/career/tracks"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackCreateRequestDto.builder()
                                .code("BE")
                                .name("Backend")
                                .description("Backend engineering")
                                .type(CareerTrackType.VERTICAL)
                                .build())))
                .andExpect(status().isCreated()));
        UUID trackId = id(created);

        mockMvc.perform(asHr(get("/api/v1/career/tracks"))
                        .param("code", "BE")
                        .param("name", "Backend")
                        .param("type", "VERTICAL")
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "code")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(get("/api/v1/career/tracks/{id}", trackId)))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(patch("/api/v1/career/tracks/{id}", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackUpdateRequestDto.builder()
                                .code("BE")
                                .name("Backend Engineering")
                                .description("Updated backend track")
                                .type(CareerTrackType.VERTICAL)
                                .build())))
                .andExpect(status().isOk());

        UUID juniorNodeId = id(read(mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/nodes", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(node(junior.id(), grade.id(), "Junior", 0, true))))
                .andExpect(status().isCreated())));
        UUID middleNodeId = id(read(mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/nodes", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(node(middle.id(), grade.id(), "Middle", 1, false))))
                .andExpect(status().isCreated())));
        UUID extraNodeId = id(read(mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/nodes", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(node(extra.id(), grade.id(), "Staff", 2, false))))
                .andExpect(status().isCreated())));

        mockMvc.perform(asHr(patch("/api/v1/career/nodes/{id}", middleNodeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackNodeUpdateRequestDto.builder()
                                .label("Middle Engineer")
                                .description("Updated node")
                                .displayOrder(1)
                                .isEntryPoint(false)
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/transitions", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(transition(juniorNodeId, middleNodeId))))
                .andExpect(status().isCreated());

        UUID removableTransitionId = id(read(mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/transitions", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(transition(middleNodeId, extraNodeId))))
                .andExpect(status().isCreated())));

        mockMvc.perform(asHr(delete("/api/v1/career/transitions/{id}", removableTransitionId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(asHr(delete("/api/v1/career/nodes/{id}", extraNodeId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/publish", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackPublicationRequestDto.builder()
                                .comment("Publish the backend track")
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(get("/api/v1/career/employees/{employeeId}/available-tracks", employeeId)))
                .andExpect(status().isOk());

        mockMvc.perform(asHr(post("/api/v1/career/tracks/{id}/archive", trackId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(CareerTrackPublicationRequestDto.builder()
                                .comment("Archive the backend track")
                                .build())))
                .andExpect(status().isOk());
    }

    private static CareerTrackNodeCreateRequestDto node(
            UUID positionId,
            UUID gradeId,
            String label,
            int displayOrder,
            boolean entryPoint
    ) {
        return CareerTrackNodeCreateRequestDto.builder()
                .positionId(positionId)
                .gradeId(gradeId)
                .label(label)
                .description(label + " node")
                .displayOrder(displayOrder)
                .isEntryPoint(entryPoint)
                .build();
    }

    private static CareerTransitionCreateRequestDto transition(UUID sourceNodeId, UUID targetNodeId) {
        return CareerTransitionCreateRequestDto.builder()
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .transitionType("PROMOTION")
                .description("Promotion")
                .estimatedMonths(6)
                .active(true)
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
