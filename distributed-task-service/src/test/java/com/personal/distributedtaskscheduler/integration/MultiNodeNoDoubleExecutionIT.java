package com.personal.distributedtaskscheduler.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.personal.distributedtaskscheduler.AbstractIntegrationTest;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.Tenant;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.factory.JobExecutorFactory;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.repository.TenantRepository;
import com.personal.distributedtaskscheduler.service.scheduled.JobDispatcherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class MultiNodeNoDoubleExecutionIT extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private org.springframework.core.env.Environment environment;

    private WireMockServer wireMockServer;
    private ConfigurableApplicationContext nodeAContext;
    private ConfigurableApplicationContext nodeBContext;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/webhook"))
                .willReturn(aResponse().withStatus(200)));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
        if (nodeAContext != null) {
            nodeAContext.close();
        }
        if (nodeBContext != null) {
            nodeBContext.close();
        }
        jobExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void concurrentDispatchers_executeDuePendingExecutionExactlyOnce() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setFirstName("Multi");
        tenant.setLastName("Node");
        tenant.setEmail("multi-node-" + UUID.randomUUID() + "@example.com");
        tenant = tenantRepository.save(tenant);

        Job job = new Job();
        job.setTenant(tenant);
        job.setName("single-dispatch");
        job.setJobType(JobType.HTTP_CALLBACK);
        job.setStatus(JobStatus.ACTIVE);
        job.setCronExpression("0 * * * * *");
        job.setWebhookUrl(wireMockServer.baseUrl() + "/webhook");
        job.setPayload(Map.of("event", "once"));
        job.setNextFireTime(Instant.now().minusSeconds(5));
        job = jobRepository.save(job);

        JobExecution execution = new JobExecution();
        execution.setJobId(job);
        execution.setStatus(JobExecutionStatus.PENDING);
        execution.setAttemptNumber(1);
        execution.setScheduledTime(Instant.now().minusSeconds(1));
        jobExecutionRepository.save(execution);

        nodeAContext = nodeContext("node-a");
        nodeBContext = nodeContext("node-b");
        JobDispatcherService nodeA = nodeAContext.getBean(JobDispatcherService.class);
        JobDispatcherService nodeB = nodeBContext.getBean(JobDispatcherService.class);

        CompletableFuture<Void> first = CompletableFuture.runAsync(nodeA::dispatchJobs);
        CompletableFuture<Void> second = CompletableFuture.runAsync(nodeB::dispatchJobs);
        CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);

        wireMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/webhook")));

        List<JobExecution> executions = jobExecutionRepository.findAllByJobId(job.getId());
        assertThat(executions).hasSize(1);
        assertThat(executions.getFirst().getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(executions).filteredOn(it -> it.getStatus() == JobExecutionStatus.SUCCESS).hasSize(1);
    }

    private ConfigurableApplicationContext nodeContext(String nodeId) {
        return new SpringApplication(com.personal.distributedtaskscheduler.DistributedTaskSchedulerApplication.class)
                .run(
                        "--spring.profiles.active=dev",
                        "--server.port=0",
                        "--spring.task.scheduling.enabled=false",
                        "--eureka.client.enabled=false",
                        "--spring.datasource.url=" + environment.getProperty("spring.datasource.url"),
                        "--spring.datasource.username=" + environment.getProperty("spring.datasource.username"),
                        "--spring.datasource.password=" + environment.getProperty("spring.datasource.password"),
                        "--spring.data.redis.host=" + environment.getProperty("spring.data.redis.host"),
                        "--spring.data.redis.port=" + environment.getProperty("spring.data.redis.port"),
                        "--scheduler.node-id=" + nodeId,
                        "--scheduler.wait-time-ms=250",
                        "--scheduler.lease-time-ms=3000",
                        "--scheduler.dispatch-interval-ms=60000",
                        "--scheduler.poll-interval-ms=60000",
                        "--scheduler.jitter-ms=0",
                        "--scheduler.misfire-check-interval-ms=60000"
                );
    }
}
