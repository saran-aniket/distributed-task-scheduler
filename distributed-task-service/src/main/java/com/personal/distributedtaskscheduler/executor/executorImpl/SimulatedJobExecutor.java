package com.personal.distributedtaskscheduler.executor.executorImpl;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.executor.JobExecutor;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SimulatedJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedJobExecutor.class);

    public SimulatedJobExecutor() {
    }

    @Override
    public ExecutionResult execute(JobExecution jobExecution, Job job) {
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
                return new ExecutionResult(failureReason, false);
            } else {
                return new ExecutionResult(null, true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult("Job execution interrupted", false);
        }
    }

    @Override
    public JobType getJobType() {
        return JobType.INTERNAL_JOB;
    }
}
