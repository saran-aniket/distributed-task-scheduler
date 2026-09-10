package com.personal.distributedtaskscheduler.executor;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.executor.executorImpl.SimulatedJobExecutor;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedTaskExecutorTest {

    @Test
    void execute_keepsEveryObservedDurationWithinExpectedRangeAcrossManyRuns() {
        SimulatedJobExecutor executor = new SimulatedJobExecutor();
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

            ExecutionResult result = executor.execute(execution, job);

            long observedDurationMs = Duration.between(before, Instant.now()).toMillis();
            observedDurationsMs.add(observedDurationMs);

            assertThat(observedDurationMs)
                    .isGreaterThanOrEqualTo(baseDurationMs)
                    .isLessThan(baseDurationMs + jitterMs + 100);

            if (shouldFail) {
                assertThat(result.isSuccess()).isFalse();
                assertThat(result.getMessage()).isEqualTo("simulated failure " + i);
            } else {
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getMessage()).isNull();
            }
        }

        assertThat(observedDurationsMs).hasSize(iterations);
        assertThat(observedDurationsMs).allMatch(duration -> duration >= baseDurationMs && duration < baseDurationMs + jitterMs + 100);
    }
}
