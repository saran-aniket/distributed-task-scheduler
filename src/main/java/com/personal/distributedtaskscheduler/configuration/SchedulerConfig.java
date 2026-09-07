package com.personal.distributedtaskscheduler.configuration;

import com.personal.distributedtaskscheduler.service.JobScannerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {
    private final JobScannerService jobScannerService;

    @Value("${scheduler.poll-interval-ms:5000}")
    private long baseDelayMs;

    @Value("${scheduler.jitter-ms:2000}")
    private long jitterMs;

    public SchedulerConfig(JobScannerService jobScannerService) {
        this.jobScannerService = jobScannerService;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                jobScannerService::scanJobs,
                triggerContext -> {
                    Instant lastCompletion = triggerContext.lastCompletion() != null
                            ? triggerContext.lastCompletion()
                            : Instant.now();
                    long jitter = ThreadLocalRandom.current().nextLong(0, jitterMs + 1);
                    return lastCompletion.plusMillis(baseDelayMs + jitter);
                }
        );
    }
}
