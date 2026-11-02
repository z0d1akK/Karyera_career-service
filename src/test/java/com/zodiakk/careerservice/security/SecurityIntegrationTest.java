package com.zodiakk.careerservice.security;

import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Security")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("rejects an anonymous request")
    void shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/career/tracks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects a request with a malformed user id")
    void shouldRejectMalformedUserId() throws Exception {
        mockMvc.perform(get("/api/v1/career/tracks")
                        .header(USER_ID_HEADER, "not-a-uuid")
                        .header(USER_ROLE_HEADER, "HR")
                        .header(USER_EMAIL_HEADER, "hr@company.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects a request with incomplete gateway headers")
    void shouldRejectIncompleteHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/career/tracks")
                        .header(USER_ID_HEADER, UUID.randomUUID().toString())
                        .header(USER_ROLE_HEADER, "HR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects an unauthenticated write")
    void shouldRejectAnonymousWrite() throws Exception {
        mockMvc.perform(post("/api/v1/career/tracks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("serves the health endpoint anonymously")
    void shouldServeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("allows CORS preflight requests anonymously")
    void shouldAllowOptionsRequests() throws Exception {
        mockMvc.perform(options("/api/v1/career/tracks")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("requires an internal key for the internal API")
    void shouldProtectInternalApi() throws Exception {
        mockMvc.perform(get("/internal/v1/career/requirements/current")
                        .param("positionId", UUID.randomUUID().toString())
                        .param("gradeId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/internal/v1/career/requirements/current")
                        .header(INTERNAL_KEY_HEADER, "wrong-key")
                        .param("positionId", UUID.randomUUID().toString())
                        .param("gradeId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("does not accept the internal key on the public API")
    void shouldNotAuthenticatePublicApiWithInternalKey() throws Exception {
        mockMvc.perform(get("/api/v1/career/tracks")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("forbids track creation for an employee")
    void shouldForbidTrackCreationForEmployee() throws Exception {
        mockMvc.perform(asEmployee(post("/api/v1/career/tracks"), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"BE","name":"Backend","type":"VERTICAL"}
                                """))
                .andExpect(status().isForbidden());
    }
}
