package com.zodiakk.careerservice.talent.controller;

import com.zodiakk.careerservice.career.dto.request.action.PositionRequirementUpsertRequestDto;
import com.zodiakk.careerservice.career.dto.request.action.RequiredSkillItemDto;
import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.enums.ReserveDecisionType;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningResultDto;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartRequestDto;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartResponseDto;
import com.zodiakk.careerservice.integration.analytics.dto.TalentScreeningDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.talent.dto.request.action.ReserveCandidateDecisionCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.action.ReserveCandidateNominationRequestDto;
import com.zodiakk.careerservice.talent.dto.request.action.TalentReserveTransitionRequestDto;
import com.zodiakk.careerservice.talent.dto.request.create.TalentReserveCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.update.TalentReserveUpdateRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Talent reserves and candidates")
class TalentReserveControllerIntegrationTest extends AbstractIntegrationTest {

    private UUID hrUserId;

    @BeforeEach
    void registerHrActor() {
        hrUserId = UUID.randomUUID();
        registerEmployee(hrUserId);
    }

    @Test
    @DisplayName("covers every talent reserve and candidate endpoint")
    void shouldCoverEveryTalentReserveEndpoint() throws Exception {
        ReferenceLookupItemDto position = registerReference("Engineering Manager");
        ReferenceLookupItemDto grade = registerReference("M2");
        ReferenceLookupItemDto skill = registerReference("Leadership");
        UUID nomineeId = registerEmployee(UUID.randomUUID()).id();
        UUID jobId = UUID.randomUUID();

        publishRequirement(position.id(), grade.id(), skill.id());
        stubScreening(jobId, position.id(), grade.id(), nomineeId);

        JsonNode created = read(mockMvc.perform(hr(post("/api/v1/career/talent-reserves"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(TalentReserveCreateRequestDto.builder()
                                .name("Leadership reserve")
                                .description("Managers for the next year")
                                .targetPositionId(position.id())
                                .targetGradeId(grade.id())
                                .effectiveFrom(LocalDate.of(2026, 1, 1))
                                .effectiveTo(LocalDate.of(2026, 12, 31))
                                .build())))
                .andExpect(status().isCreated()));
        UUID reserveId = id(created);

        mockMvc.perform(hr(get("/api/v1/career/talent-reserves"))
                        .param("name", "Leadership")
                        .param("targetPositionId", position.id().toString())
                        .param("targetGradeId", grade.id().toString())
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "name")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        mockMvc.perform(hr(get("/api/v1/career/talent-reserves/{id}", reserveId)))
                .andExpect(status().isOk());

        mockMvc.perform(hr(patch("/api/v1/career/talent-reserves/{id}", reserveId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(TalentReserveUpdateRequestDto.builder()
                                .name("Leadership reserve 2026")
                                .description("Updated reserve")
                                .build())))
                .andExpect(status().isOk());

        mockMvc.perform(hr(post("/api/v1/career/talent-reserves/{id}/activate", reserveId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(TalentReserveTransitionRequestDto.builder()
                                .comment("Open the reserve")
                                .build())))
                .andExpect(status().isOk());

        JsonNode candidate = read(mockMvc.perform(hr(post("/api/v1/career/talent-reserves/{id}/candidates", reserveId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(ReserveCandidateNominationRequestDto.builder()
                                .employeeId(nomineeId)
                                .assessmentId(UUID.randomUUID())
                                .comment("Ready for the reserve")
                                .build())))
                .andExpect(status().isCreated()));
        UUID candidateId = id(candidate);

        mockMvc.perform(hr(get("/api/v1/career/talent-reserves/{id}/candidates", reserveId))
                        .param("reserveId", reserveId.toString())
                        .param("employeeId", nomineeId.toString())
                        .param("status", "NOMINATED")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "nominatedAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk());

        mockMvc.perform(hr(get("/api/v1/career/reserve-candidates/{id}", candidateId)))
                .andExpect(status().isOk());

        mockMvc.perform(hr(post("/api/v1/career/reserve-candidates/{id}/decisions", candidateId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(ReserveCandidateDecisionCreateRequestDto.builder()
                                .decision(ReserveDecisionType.APPROVE)
                                .comment("Include in the reserve")
                                .assessmentId(UUID.randomUUID())
                                .expectedVersion(version(candidate))
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(hr(get("/api/v1/career/reserve-candidates/{id}/decisions", candidateId))
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "decidedAt")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk());

        mockMvc.perform(hr(post("/api/v1/career/talent-reserves/{id}/screenings", reserveId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));

        mockMvc.perform(hr(get("/api/v1/career/talent-reserves/{id}/screenings/{jobId}", reserveId, jobId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(hr(get("/api/v1/career/talent-reserves/{id}/analysis", reserveId)))
                .andExpect(status().isOk());

        mockMvc.perform(hr(post("/api/v1/career/talent-reserves/{id}/close", reserveId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(TalentReserveTransitionRequestDto.builder()
                                .comment("Close the reserve")
                                .build())))
                .andExpect(status().isOk());
    }

    private void publishRequirement(UUID positionId, UUID gradeId, UUID skillId) throws Exception {
        JsonNode created = read(mockMvc.perform(hr(post("/api/v1/career/requirements"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(PositionRequirementUpsertRequestDto.builder()
                                .positionId(positionId)
                                .gradeId(gradeId)
                                .description("Manager requirements")
                                .minExperienceMonths(36)
                                .skills(List.of(RequiredSkillItemDto.builder()
                                        .skillId(skillId)
                                        .requiredLevel(3)
                                        .weight(new BigDecimal("2.00"))
                                        .mandatory(true)
                                        .description("Leadership")
                                        .build()))
                                .build())))
                .andExpect(status().isCreated()));

        mockMvc.perform(hr(post("/api/v1/career/requirements/{id}/publish", id(created))))
                .andExpect(status().isOk());
    }

    private void stubScreening(UUID jobId, UUID positionId, UUID gradeId, UUID employeeId) {
        lenient().when(analyticsScreeningClient.startScreening(any(ScreeningStartRequestDto.class)))
                .thenReturn(new ScreeningStartResponseDto(jobId, "RUNNING"));

        lenient().when(analyticsScreeningClient.getScreening(any(UUID.class)))
                .thenReturn(new TalentScreeningDto(
                        UUID.randomUUID(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        jobId,
                        UUID.randomUUID(),
                        positionId,
                        gradeId,
                        UUID.randomUUID(),
                        1,
                        "v1",
                        "COMPLETED",
                        1,
                        1,
                        0,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        List.of(new ScreeningResultDto(
                                UUID.randomUUID(),
                                OffsetDateTime.now(ZoneOffset.UTC),
                                jobId,
                                employeeId,
                                UUID.randomUUID(),
                                1,
                                new BigDecimal("87.50"),
                                "ELIGIBLE",
                                true,
                                null
                        ))
                ));
    }

    private <B extends AbstractMockHttpServletRequestBuilder<B>> B hr(B builder) {
        return asUser(builder, hrUserId, "HR");
    }

    private JsonNode read(ResultActions actions) throws Exception {
        MvcResult result = actions.andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static UUID id(JsonNode node) {
        return UUID.fromString(node.get("id").asString());
    }

    private static long version(JsonNode node) {
        return node.get("version").longValue();
    }
}
