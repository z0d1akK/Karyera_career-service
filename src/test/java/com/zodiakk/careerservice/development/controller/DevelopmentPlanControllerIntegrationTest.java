package com.zodiakk.careerservice.development.controller;

import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import com.zodiakk.careerservice.common.enums.DevelopmentActivityType;
import com.zodiakk.careerservice.common.enums.PlanReviewDecision;
import com.zodiakk.careerservice.development.dto.request.action.ActivityEvidenceCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivityCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivitySubmitRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentActivityVerificationRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanCompletionRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanReviewRequestDto;
import com.zodiakk.careerservice.development.dto.request.action.DevelopmentPlanSubmitRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentActivityUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentPlanUpdateRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Development plans and activities")
class DevelopmentPlanControllerIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private static final LocalDate TARGET = LocalDate.of(2026, 12, 31);

    private UUID hrUserId;

    @BeforeEach
    void registerHrActor() {
        hrUserId = UUID.randomUUID();
        registerEmployee(hrUserId);
    }

    @Test
    @DisplayName("completes a plan after approval and activity verification")
    void shouldCompletePlanLifecycle() throws Exception {
        UUID employeeId = registerEmployee(UUID.randomUUID()).id();
        UUID skillId = registerReference("Java").id();

        JsonNode created = read(mockMvc.perform(hr(post("/api/v1/career/development-plans"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(plan(employeeId, "Backend growth"))))
                .andExpect(status().isCreated()));
        UUID planId = id(created);

        JsonNode updated = read(mockMvc.perform(hr(patch("/api/v1/career/development-plans/{id}", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(DevelopmentPlanUpdateRequestDto.builder()
                                .title("Backend growth plan")
                                .description("Updated description")
                                .build())))
                .andExpect(status().isOk()));

        JsonNode activity = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/activities", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(activity(skillId, "Pass the Java exam"))))
                .andExpect(status().isCreated()));
        UUID activityId = id(activity);

        JsonNode editedActivity = read(mockMvc.perform(hr(patch("/api/v1/career/development-activities/{id}", activityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(DevelopmentActivityUpdateRequestDto.builder()
                                .title("Pass the Java certification")
                                .description("Updated activity")
                                .dueDate(LocalDate.of(2026, 6, 1))
                                .build())))
                .andExpect(status().isOk()));

        mockMvc.perform(hr(post("/api/v1/career/development-activities/{id}/evidence", activityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(ActivityEvidenceCreateRequestDto.builder()
                                .evidenceType("EXTERNAL_URL")
                                .externalUrl("https://example.com/certificate")
                                .description("Certificate")
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(hr(get("/api/v1/career/employees/{employeeId}/development-plans", employeeId))
                        .param("employeeId", employeeId.toString())
                        .param("careerGoalId", UUID.randomUUID().toString())
                        .param("status", "DRAFT")
                        .param("startDateFrom", "2026-01-01")
                        .param("startDateTo", "2026-12-31")
                        .param("targetDateFrom", "2026-01-01")
                        .param("targetDateTo", "2026-12-31")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "title")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk());

        mockMvc.perform(hr(get("/api/v1/career/employees/{employeeId}/development-progress", employeeId)))
                .andExpect(status().isOk());

        mockMvc.perform(hr(get("/api/v1/career/development-plans/{id}", planId)))
                .andExpect(status().isOk());

        JsonNode submitted = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/submit", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(submitPlan(version(updated)))))
                .andExpect(status().isOk()));

        JsonNode approved = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/approve", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(review(PlanReviewDecision.APPROVED, "Approved", version(submitted)))))
                .andExpect(status().isOk()));

        mockMvc.perform(hr(get("/api/v1/career/development-plans/{id}/reviews", planId)))
                .andExpect(status().isOk());

        JsonNode submittedActivity = read(mockMvc.perform(hr(post(
                        "/api/v1/career/development-activities/{id}/submit",
                        activityId
                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(submitActivity(version(editedActivity)))))
                .andExpect(status().isOk()));

        mockMvc.perform(hr(post("/api/v1/career/development-activities/{id}/verify", activityId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(verify(version(submittedActivity)))))
                .andExpect(status().isOk());

        mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/complete", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(completion("Plan completed", version(approved)))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("requests changes, rejects an activity and cancels the plan")
    void shouldRequestChangesRejectActivityAndCancel() throws Exception {
        UUID employeeId = registerEmployee(UUID.randomUUID()).id();

        JsonNode created = read(mockMvc.perform(hr(post("/api/v1/career/development-plans"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(plan(employeeId, "Revision plan"))))
                .andExpect(status().isCreated()));
        UUID planId = id(created);

        JsonNode activity = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/activities", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(activity(null, "Shadow a senior engineer"))))
                .andExpect(status().isCreated()));

        JsonNode submitted = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/submit", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(submitPlan(version(created)))))
                .andExpect(status().isOk()));

        JsonNode changesRequested = read(mockMvc.perform(hr(post(
                        "/api/v1/career/development-plans/{id}/request-changes",
                        planId
                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(review(
                                PlanReviewDecision.CHANGES_REQUESTED,
                                "Add a due date",
                                version(submitted)
                        ))))
                .andExpect(status().isOk()));

        JsonNode resubmitted = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/submit", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(submitPlan(version(changesRequested)))))
                .andExpect(status().isOk()));

        JsonNode approved = read(mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/approve", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(review(PlanReviewDecision.APPROVED, "Approved", version(resubmitted)))))
                .andExpect(status().isOk()));

        JsonNode submittedActivity = read(mockMvc.perform(hr(post(
                        "/api/v1/career/development-activities/{id}/submit",
                        id(activity)
                ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(submitActivity(version(activity)))))
                .andExpect(status().isOk()));

        mockMvc.perform(hr(post("/api/v1/career/development-activities/{id}/reject", id(activity)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(verify(version(submittedActivity)))))
                .andExpect(status().isOk());

        mockMvc.perform(hr(post("/api/v1/career/development-plans/{id}/cancel", planId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(completion("Cancel the plan", version(approved)))))
                .andExpect(status().isOk());
    }

    private <B extends AbstractMockHttpServletRequestBuilder<B>> B hr(B builder) {
        return asUser(builder, hrUserId, "HR");
    }

    private static DevelopmentPlanCreateRequestDto plan(UUID employeeId, String title) {
        return DevelopmentPlanCreateRequestDto.builder()
                .employeeId(employeeId)
                .title(title)
                .description("Individual development plan")
                .startDate(START)
                .targetDate(TARGET)
                .build();
    }

    private static DevelopmentActivityCreateRequestDto activity(UUID skillId, String title) {
        return DevelopmentActivityCreateRequestDto.builder()
                .skillId(skillId)
                .title(title)
                .description(title)
                .activityType(DevelopmentActivityType.CERTIFICATION)
                .dueDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    private static DevelopmentPlanSubmitRequestDto submitPlan(long expectedVersion) {
        return DevelopmentPlanSubmitRequestDto.builder()
                .comment("Ready for review")
                .expectedVersion(expectedVersion)
                .build();
    }

    private static DevelopmentPlanReviewRequestDto review(
            PlanReviewDecision decision,
            String comment,
            long expectedVersion
    ) {
        return DevelopmentPlanReviewRequestDto.builder()
                .decision(decision)
                .comment(comment)
                .expectedVersion(expectedVersion)
                .build();
    }

    private static DevelopmentPlanCompletionRequestDto completion(String comment, long expectedVersion) {
        return DevelopmentPlanCompletionRequestDto.builder()
                .comment(comment)
                .expectedVersion(expectedVersion)
                .build();
    }

    private static DevelopmentActivitySubmitRequestDto submitActivity(long expectedVersion) {
        return DevelopmentActivitySubmitRequestDto.builder()
                .comment("Activity is done")
                .expectedVersion(expectedVersion)
                .build();
    }

    private static DevelopmentActivityVerificationRequestDto verify(long expectedVersion) {
        return DevelopmentActivityVerificationRequestDto.builder()
                .comment("Checked")
                .expectedVersion(expectedVersion)
                .build();
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
