package com.personal.distributedtaskscheduler.executor.executorImpl;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.executor.JobExecutor;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Component
public class HTTPCallbackExecutor implements JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(HTTPCallbackExecutor.class);
    private final RestClient restClient;
    private final JobExecutionRepository jobExecutionRepository;

    public HTTPCallbackExecutor(RestClient restClient, JobExecutionRepository jobExecutionRepository) {
        this.restClient = restClient;
        this.jobExecutionRepository = jobExecutionRepository;
    }
    @Override
    public JobExecutionStatus execute(JobExecution jobExecution, Job job) {
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
            jobExecution.setStatus(JobExecutionStatus.SUCCESS);
            jobExecution.setCompletedAt(Instant.now());
        } catch (RestClientResponseException e) {
            jobExecution.setStatus(JobExecutionStatus.FAILED);
            jobExecution.setErrorMessage("Webhook returned HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            jobExecution.setStatus(JobExecutionStatus.FAILED);
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("request cancelled")) {
                jobExecution.setErrorMessage("Webhook invocation timed out");
            } else {
                jobExecution.setErrorMessage("Webhook invocation failed: " + message);
            }
        }
        jobExecutionRepository.save(jobExecution);
        return jobExecution.getStatus();
    }

    @Override
    public JobType getJobType() {
        return JobType.HTTP_CALLBACK;
    }
}
