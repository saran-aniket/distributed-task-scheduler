package com.personal.distributedtaskscheduler.executor;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.executor.executorImpl.SimulatedJobExecutor;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SimulatedTaskExecutorTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @Test
    void execute_keepsEveryObservedDurationWithinExpectedRangeAcrossManyRuns() {
        SimulatedJobExecutor executor = new SimulatedJobExecutor(jobExecutionRepository);
        List<Long> observedDurationsMs = new ArrayList<>();
        int iterations = 100;
        long baseDurationMs = 20;
        long jitterMs = 30;

        for (int i = 0; i < iterations; i++) {
            boolean shouldFail = i % 4 == 0;
            Job job = new Job();
            job.setName("simulated-job-" + i);
            job.setPayload(Map.of(
                    "baseDurationMs", baseDurationMs,
                    "jitterMs", jitterMs,
                    "shouldFail", shouldFail,
                    "failureReason", "simulated failure " + i
            ));

            JobExecution execution = new JobExecution();
            Instant before = Instant.now();

            JobExecutionStatus status = executor.execute(execution, job);

            Instant completedAt = execution.getCompletedAt();
            assertThat(completedAt).isNotNull();
            long observedDurationMs = Duration.between(before, completedAt).toMillis();
            observedDurationsMs.add(observedDurationMs);

            assertThat(observedDurationMs)
                    .isGreaterThanOrEqualTo(baseDurationMs)
                    .isLessThan(baseDurationMs + jitterMs + 100);

            if (shouldFail) {
                assertThat(status).isEqualTo(JobExecutionStatus.FAILED);
                assertThat(execution.getErrorMessage()).isEqualTo("simulated failure " + i);
            } else {
                assertThat(status).isEqualTo(JobExecutionStatus.SUCCESS);
                assertThat(execution.getErrorMessage()).isNull();
            }
        }

        assertThat(observedDurationsMs).hasSize(iterations);
        assertThat(observedDurationsMs).allMatch(duration -> duration >= baseDurationMs && duration < baseDurationMs + jitterMs + 100);
    }
}
