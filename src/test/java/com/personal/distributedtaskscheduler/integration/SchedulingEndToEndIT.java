package com.personal.distributedtaskscheduler.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.personal.distributedtaskscheduler.AbstractIntegrationTest;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.Tenant;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.repository.TenantRepository;
import com.personal.distributedtaskscheduler.service.scheduled.JobDispatcherService;
import com.personal.distributedtaskscheduler.service.scheduled.JobScannerService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class SchedulingEndToEndIT extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private JobScannerService jobScannerService;

    @Autowired
    private JobDispatcherService jobDispatcherService;

    @MockitoBean
    private ThreadPoolTaskScheduler taskScheduler;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        jobExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void scannerAndDispatcher_processPastDueWebhookJobToSuccessfulCompletion() {
        wireMockServer.stubFor(post(urlEqualTo("/webhook"))
                .willReturn(aResponse().withStatus(200)));

        Tenant tenant = new Tenant();
        tenant.setFirstName("Test");
        tenant.setLastName("Tenant");
        tenant.setEmail("scheduler-e2e@example.com");
        tenant = tenantRepository.save(tenant);

        Job job = new Job();
        job.setTenant(tenant);
        job.setName("past-due-webhook-job");
        job.setJobType(JobType.HTTP_CALLBACK);
        job.setStatus(JobStatus.ACTIVE);
        job.setCronExpression("0 * * * * *");
        job.setWebhookUrl(wireMockServer.baseUrl() + "/webhook");
        job.setPayload(Map.of("event", "ping"));
        Instant originalNextFireTime = Instant.now().minusSeconds(60);
        job.setNextFireTime(originalNextFireTime);
        Job savedJob = jobRepository.save(job);

        jobScannerService.scanJobs();
        jobDispatcherService.dispatchJobs();

        List<JobExecution> executions = jobExecutionRepository.findAllByJobId(savedJob.getId());
        assertThat(executions).hasSize(1);

        JobExecution execution = executions.getFirst();
        assertThat(execution.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(execution.getScheduledTime()).isEqualTo(originalNextFireTime);
        assertThat(execution.getCompletedAt()).isNotNull();

        Job updatedJob = jobRepository.findById(savedJob.getId()).orElseThrow();
        assertThat(updatedJob.getNextFireTime()).isAfter(execution.getScheduledTime());
    }
}
