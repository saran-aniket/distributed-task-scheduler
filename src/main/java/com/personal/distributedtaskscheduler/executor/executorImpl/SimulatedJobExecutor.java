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

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SimulatedJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedJobExecutor.class);
    private final JobExecutionRepository jobExecutionRepository;

    public SimulatedJobExecutor(JobExecutionRepository jobExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
    }

    @Override
    public JobExecutionStatus execute(JobExecution jobExecution, Job job) {
        log.info("Executing simulated job: {}", job.getName());
        Map<String, Object> payload = job.getPayload();
        long baseDurationMs = ((Number) payload.get("baseDurationMs")).longValue();
        long jitterDurationMs = ((Number) payload.get("jitterMs")).longValue();
        long durationMs = baseDurationMs + ThreadLocalRandom.current().nextLong(jitterDurationMs + 1);
        boolean shouldFail = (boolean) payload.get("shouldFail");
        String failureReason = (String) payload.get("failureReason");
        Instant startedAt = Instant.now();
        try {
            Thread.sleep(durationMs);
            if (shouldFail) {
                jobExecution.setStatus(JobExecutionStatus.FAILED);
                jobExecution.setErrorMessage(failureReason);
            } else {
                jobExecution.setStatus(JobExecutionStatus.SUCCESS);
                jobExecution.setCompletedAt(Instant.now());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jobExecution.setStatus(JobExecutionStatus.FAILED);
            jobExecution.setErrorMessage("Job execution interrupted");
        }
        if (jobExecution.getCompletedAt() == null) {
            jobExecution.setCompletedAt(Instant.now());
        }
        jobExecution.setStartedAt(startedAt);
        jobExecutionRepository.save(jobExecution);
        return jobExecution.getStatus();
    }

    @Override
    public JobType getJobType() {
        return JobType.INTERNAL_JOB;
    }
}
