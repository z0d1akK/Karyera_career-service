package com.zodiakk.careerservice.common;

import com.zodiakk.careerservice.TestcontainersConfiguration;
import com.zodiakk.careerservice.common.constants.Headers;
import com.zodiakk.careerservice.integration.analytics.client.AnalyticsScreeningClient;
import com.zodiakk.careerservice.integration.employee.client.EmployeeInternalClient;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeAccessScopeDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupRequestDto;
import com.zodiakk.careerservice.integration.employee.dto.EmployeeLookupResponseDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupItemDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupRequestDto;
import com.zodiakk.careerservice.integration.employee.dto.ReferenceLookupResponseDto;
import com.zodiakk.careerservice.integration.project.client.ProjectInternalClient;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String USER_ID_HEADER = Headers.USER_ID;

    protected static final String USER_ROLE_HEADER = Headers.USER_ROLE;

    protected static final String USER_EMAIL_HEADER = Headers.USER_EMAIL;

    protected static final String INTERNAL_KEY_HEADER = Headers.INTERNAL_KEY;

    protected static final String INTERNAL_API_KEY = "test-internal-api-key";

    private final Map<UUID, EmployeeLookupItemDto> employeesById = new HashMap<>();

    private final Map<UUID, EmployeeLookupItemDto> employeesByUserId = new HashMap<>();

    private final Map<UUID, ReferenceLookupItemDto> referencesById = new HashMap<>();

    @MockitoBean
    protected EmployeeInternalClient employeeInternalClient;

    @MockitoBean
    protected ProjectInternalClient projectInternalClient;

    @MockitoBean
    protected AnalyticsScreeningClient analyticsScreeningClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                () -> TestcontainersConfiguration.kafka().getBootstrapServers()
        );
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE activity_evidence, plan_reviews, development_activities, development_plans,
                    reserve_decisions, reserve_candidates, talent_reserves, required_skills,
                    position_requirements, employee_career_goals, career_transitions, career_track_nodes,
                    career_track_revisions, career_tracks, processed_events, outbox_events
                    CASCADE
                """);

        employeesById.clear();
        employeesByUserId.clear();
        referencesById.clear();
        stubEmployeeClient();
    }

    protected String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    protected EmployeeLookupItemDto registerEmployee(UUID userId) {
        EmployeeLookupItemDto item = new EmployeeLookupItemDto(
                UUID.randomUUID(),
                "Ivan Petrov",
                true,
                null,
                null
        );
        employeesById.put(item.id(), item);
        if (userId != null) {
            employeesByUserId.put(userId, item);
        }
        return item;
    }

    protected ReferenceLookupItemDto registerReference(String name) {
        ReferenceLookupItemDto item = new ReferenceLookupItemDto(UUID.randomUUID(), name, true);
        referencesById.put(item.id(), item);
        return item;
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asUser(
            B builder,
            UUID userId,
            String role
    ) {
        return builder
                .header(USER_ID_HEADER, userId.toString())
                .header(USER_ROLE_HEADER, role)
                .header(USER_EMAIL_HEADER, "user-" + userId + "@company.com");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asHr(B builder) {
        return asUser(builder, UUID.randomUUID(), "HR");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asAdmin(B builder) {
        return asUser(builder, UUID.randomUUID(), "ADMIN");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asEmployee(B builder, UUID userId) {
        return asUser(builder, userId, "EMPLOYEE");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asManager(B builder) {
        return asUser(builder, UUID.randomUUID(), "MANAGER");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asInternal(B builder) {
        return builder.header(INTERNAL_KEY_HEADER, INTERNAL_API_KEY);
    }

    private void stubEmployeeClient() {
        lenient().when(employeeInternalClient.lookupEmployees(any(EmployeeLookupRequestDto.class)))
                .thenAnswer(invocation -> {
                    EmployeeLookupRequestDto request = invocation.getArgument(0);
                    List<EmployeeLookupItemDto> found = request.employeeIds() == null
                            ? List.of()
                            : request.employeeIds().stream()
                            .filter(employeesById::containsKey)
                            .map(employeesById::get)
                            .toList();
                    return new EmployeeLookupResponseDto(found);
                });

        lenient().when(employeeInternalClient.lookupPositions(any(ReferenceLookupRequestDto.class)))
                .thenAnswer(invocation -> referenceResponse(invocation.getArgument(0)));
        lenient().when(employeeInternalClient.lookupGrades(any(ReferenceLookupRequestDto.class)))
                .thenAnswer(invocation -> referenceResponse(invocation.getArgument(0)));
        lenient().when(employeeInternalClient.lookupSkills(any(ReferenceLookupRequestDto.class)))
                .thenAnswer(invocation -> referenceResponse(invocation.getArgument(0)));

        lenient().when(employeeInternalClient.getByUserId(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    EmployeeLookupItemDto item = employeesByUserId.get(userId);
                    if (item == null) {
                        throw notFound("/internal/v1/employees/by-user/" + userId);
                    }
                    return item;
                });

        lenient().when(employeeInternalClient.getAccessScope(any(UUID.class), any()))
                .thenAnswer(invocation -> new EmployeeAccessScopeDto(
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        false,
                        true,
                        true
                ));
    }

    private ReferenceLookupResponseDto referenceResponse(ReferenceLookupRequestDto request) {
        List<ReferenceLookupItemDto> found = request.ids() == null
                ? List.of()
                : request.ids().stream()
                .filter(referencesById::containsKey)
                .map(referencesById::get)
                .toList();
        return new ReferenceLookupResponseDto(found);
    }

    private FeignException.NotFound notFound(String path) {
        return new FeignException.NotFound(
                "missing",
                Request.create(
                        Request.HttpMethod.GET,
                        path,
                        Map.of(),
                        null,
                        StandardCharsets.UTF_8,
                        null
                ),
                null,
                Map.of()
        );
    }
}
