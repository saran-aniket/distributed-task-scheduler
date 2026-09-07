package com.personal.distributedtaskscheduler.controller;

import com.personal.distributedtaskscheduler.AbstractIntegrationTest;
import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import com.personal.distributedtaskscheduler.dto.JobResponseDTO;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for JobController with real database and Spring context.
 * Performs full round-trip testing: POST /api/v1/jobs, GET /api/v1/jobs/{id}
 * and verifies request/response contracts and database persistence.
 */
class JobControllerIT extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JobRepository jobRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void postCreateJob_withValidRequest_persistsJobAndReturnsOk() throws Exception {
        // Arrange
        CreateJobRequestDTO request = new CreateJobRequestDTO();
        request.setName("full-roundtrip-job");
        request.setJobType("HTTP_CALLBACK");
        request.setCronExpression("0 0 * * *");
        request.setPayLoad("{\"url\":\"https://webhook.example.com\",\"method\":\"POST\"}");
        request.setWebhookUrl("https://callback.example.com/webhook");

        String requestJson = new ObjectMapper().writeValueAsString(request);

        // Act & Assert - POST creates job
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("full-roundtrip-job"))
                .andExpect(jsonPath("$.jobType").value("HTTP_CALLBACK"))
                .andExpect(jsonPath("$.cronExpression").value("0 0 * * *"))
                .andReturn();

        // Verify job was persisted to DB
        assertThat(jobRepository.findAll()).isNotEmpty();
    }

    @Test
    void postThenGet_roundTrip_requestResponseConsistency() throws Exception {
        // Arrange
        CreateJobRequestDTO createRequest = new CreateJobRequestDTO();
        createRequest.setName("roundtrip-verification");
        createRequest.setJobType("INTERNAL_JOB");
        createRequest.setCronExpression("*/30 * * * *");
        createRequest.setPayLoad("{\"task\":\"cleanup\"}");

        String requestJson = new ObjectMapper().writeValueAsString(createRequest);

        // Act & Assert - POST
        String postResponse = mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("roundtrip-verification"))
                .andExpect(jsonPath("$.jobType").value("INTERNAL_JOB"))
                .andExpect(jsonPath("$.cronExpression").value("*/30 * * * *"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract job ID from response
        JobResponseDTO createdJob = new ObjectMapper().readValue(postResponse, JobResponseDTO.class);
        String jobId = createdJob.getId();

        // Act & Assert - GET the same job back
        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.name").value("roundtrip-verification"))
                .andExpect(jsonPath("$.jobType").value("INTERNAL_JOB"))
                .andExpect(jsonPath("$.cronExpression").value("*/30 * * * *"));
    }

    @Test
    void postCreateJob_withMalformedCronExpression_returnsBadRequest() throws Exception {
        // Arrange - invalid cron: 6 fields instead of 5
        CreateJobRequestDTO request = new CreateJobRequestDTO();
        request.setName("bad-cron-job");
        request.setJobType("HTTP_CALLBACK");
        request.setCronExpression("0 0 * * * *");  // 6 fields - INVALID
        request.setPayLoad("{}");

        String requestJson = new ObjectMapper().writeValueAsString(request);

        // Act & Assert - should reject at controller validation boundary
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateJob_withInvalidCharactersInCron_returnsBadRequest() throws Exception {
        // Arrange - cron with invalid characters
        CreateJobRequestDTO request = new CreateJobRequestDTO();
        request.setName("invalid-chars-cron");
        request.setJobType("HTTP_CALLBACK");
        request.setCronExpression("0 0 @ * *");  // @ is not allowed
        request.setPayLoad("{}");

        String requestJson = new ObjectMapper().writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateJob_withMissingRequiredFields_returnsBadRequest() throws Exception {
        // Arrange - missing jobType
        CreateJobRequestDTO request = new CreateJobRequestDTO();
        request.setName("missing-fields-job");
        // jobType is null/missing
        request.setCronExpression("0 0 * * *");
        request.setPayLoad("{}");

        String requestJson = new ObjectMapper().writeValueAsString(request);

        // Act & Assert
        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postCreateJob_withValidCronVariations_allSucceed() throws Exception {
        // Table of valid cron expressions that should all be accepted
        String[] validCrons = {
                "0 0 * * *",           // midnight every day
                "*/15 * * * *",        // every 15 minutes
                "0 9 1 * *",           // 9 AM on first day of month
                "30 2 * * 1-5",        // 2:30 AM weekdays
                "0 0,12 * * *",        // midnight and noon
        };

        for (String cron : validCrons) {
            // Arrange
            CreateJobRequestDTO request = new CreateJobRequestDTO();
            request.setName("cron-variant-" + cron.replace(" ", "-"));
            request.setJobType("HTTP_CALLBACK");
            request.setCronExpression(cron);
            request.setPayLoad("{}");

            String requestJson = new ObjectMapper().writeValueAsString(request);

            // Act & Assert - all should succeed
            mockMvc.perform(post("/api/v1/jobs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cronExpression").value(cron));
        }
    }

    @Test
    void getJobs_withPagination_returnsPagedResults() throws Exception {
        // Act & Assert - GET all jobs with default pagination
        mockMvc.perform(get("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }
}
