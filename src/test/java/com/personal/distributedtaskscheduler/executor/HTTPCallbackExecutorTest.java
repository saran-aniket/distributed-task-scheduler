package com.personal.distributedtaskscheduler.executor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.executor.executorImpl.HTTPCallbackExecutor;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HTTPCallbackExecutorTest {

    private WireMockServer wireMockServer;

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    private HTTPCallbackExecutor httpCallbackExecutor;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(1))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(1));

        httpCallbackExecutor = new HTTPCallbackExecutor(
                RestClient.builder().requestFactory(requestFactory).build(),
                jobExecutionRepository
        );
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void execute_marksExecutionSuccessfulWhenWebhookReturns200() {
        wireMockServer.stubFor(post(urlEqualTo("/success"))
                .willReturn(aResponse().withStatus(200)));

        JobExecution jobExecution = new JobExecution();
        Job job = webhookJob("/success");

        JobExecutionStatus status = httpCallbackExecutor.execute(jobExecution, job);

        assertThat(status).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(jobExecution.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(jobExecution.getCompletedAt()).isNotNull();
        assertThat(jobExecution.getErrorMessage()).isNull();
        verify(jobExecutionRepository).save(jobExecution);
    }

    @Test
    void execute_marksExecutionFailedWhenWebhookReturns500() {
        wireMockServer.stubFor(post(urlEqualTo("/server-error"))
                .willReturn(aResponse().withStatus(500).withBody("upstream exploded")));

        JobExecution jobExecution = new JobExecution();
        Job job = webhookJob("/server-error");

        JobExecutionStatus status = httpCallbackExecutor.execute(jobExecution, job);

        assertThat(status).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(jobExecution.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(jobExecution.getCompletedAt()).isNull();
        assertThat(jobExecution.getErrorMessage()).contains("500").contains("upstream exploded");
        verify(jobExecutionRepository).save(jobExecution);
    }

    @Test
    @Timeout(5)
    void execute_marksExecutionFailedWhenWebhookTimesOut() {
        wireMockServer.stubFor(post(urlEqualTo("/slow"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(3_000)));

        JobExecution jobExecution = new JobExecution();
        Job job = webhookJob("/slow");

        JobExecutionStatus status = httpCallbackExecutor.execute(jobExecution, job);

        assertThat(status).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(jobExecution.getStatus()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(jobExecution.getCompletedAt()).isNull();
        assertThat(jobExecution.getErrorMessage()).contains("timed out");
        verify(jobExecutionRepository).save(jobExecution);
    }

    private Job webhookJob(String path) {
        Job job = new Job();
        job.setName("webhook-job");
        job.setWebhookUrl(wireMockServer.baseUrl() + path);
        job.setPayload(Map.of("hello", "world"));
        return job;
    }
}