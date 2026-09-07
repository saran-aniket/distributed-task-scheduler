package com.personal.distributedtaskscheduler.executor.executorImpl;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.executor.JobExecutor;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

@Component
public class HTTPCallbackExecutor implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(HTTPCallbackExecutor.class);
    private final RestClient restClient;

    public HTTPCallbackExecutor(RestClient restClient) {
        this.restClient = restClient;
    }
    @Override
    public ExecutionResult execute(JobExecution jobExecution, Job job) {
        log.info("Executing HTTP callback job: {}", job.getName());
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String payload = objectMapper.writeValueAsString(job.getPayload());
            restClient.post()
                    .uri(job.getWebhookUrl())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return new ExecutionResult(null, true);
        } catch (RestClientResponseException e) {
            String errorMessage = "Webhook returned HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString();
            return new ExecutionResult(errorMessage, false);
        } catch (Exception e) {
            log.error("Error executing HTTP callback job: {}", e.getMessage(), e);
            String errorMessage = "Error executing HTTP callback job: " + e.getMessage();
            return new ExecutionResult(errorMessage, false);
        }
    }

    @Override
    public JobType getJobType() {
        return JobType.HTTP_CALLBACK;
    }
}
