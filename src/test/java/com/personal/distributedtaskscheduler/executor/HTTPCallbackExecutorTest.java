package com.personal.distributedtaskscheduler.executor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.executor.executorImpl.HTTPCallbackExecutor;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

class HTTPCallbackExecutorTest {

    private WireMockServer wireMockServer;
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
                RestClient.builder().requestFactory(requestFactory).build()
        );
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void execute_returnsSuccessWhenWebhookReturns200() {
        wireMockServer.stubFor(post(urlEqualTo("/success"))
                .willReturn(aResponse().withStatus(200)));

        JobExecution jobExecution = new JobExecution();
        Job job = webhookJob("/success");

        ExecutionResult result = httpCallbackExecutor.execute(jobExecution, job);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    void execute_returnsFailureWhenWebhookReturns500() {
        wireMockServer.stubFor(post(urlEqualTo("/server-error"))
                .willReturn(aResponse().withStatus(500).withBody("upstream exploded")));

        JobExecution jobExecution = new JobExecution();
        Job job = webhookJob("/server-error");

        ExecutionResult result = httpCallbackExecutor.execute(jobExecution, job);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("500").contains("upstream exploded");
    }

    @Test
    @Timeout(5)
    void execute_returnsFailureWhenWebhookTimesOut() {
        wireMockServer.stubFor(post(urlEqualTo("/slow"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(3_000)));

        JobExecution jobExecution = new JobExecution();
        Job job = webhookJob("/slow");

        ExecutionResult result = httpCallbackExecutor.execute(jobExecution, job);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Error") .containsIgnoringCase("executing");
    }

    private Job webhookJob(String path) {
        Job job = new Job();
        job.setName("webhook-job");
        job.setWebhookUrl(wireMockServer.baseUrl() + path);
        job.setPayload(Map.of("hello", "world"));
        return job;
    }
}