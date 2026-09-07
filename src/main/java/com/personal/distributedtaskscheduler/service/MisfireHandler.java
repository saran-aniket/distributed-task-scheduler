package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobMisfirePolicy;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MisfireHandler {

    private static final Logger log = LoggerFactory.getLogger(MisfireHandler.class);
    @Value("${scheduler.execution-timeout-ms:15000}")
    private int executionTimeOut;

    private final JobExecutionRepository jobExecutionRepository;

    public MisfireHandler(JobExecutionRepository jobExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
    }

    @Scheduled(fixedRateString = "${scheduler.misfire-check-interval-ms:15000}")
    public void handleMisfire() {
        List<JobExecution> runningJobs = jobExecutionRepository.findJobExecutionsByStatusIn((List.of(JobExecutionStatus.RUNNING, JobExecutionStatus.CLAIMED)));
        runningJobs.forEach(jobExecution -> {
            log.info("Checking misfire for job execution {} - {}", jobExecution.getJobId().getName(), jobExecution.getId());
            if(jobExecution.getStartedAt().plusMillis(executionTimeOut).isBefore(java.time.Instant.now())){
                switch (jobExecution.getJobId().getMisfirePolicy()){
                    case JobMisfirePolicy.SKIP -> {
                        jobExecution.setStatus(JobExecutionStatus.TIMED_OUT);
                        jobExecutionRepository.save(jobExecution);
                    }

                    case JobMisfirePolicy.FIRE_ONCE -> {
                        jobExecution.setStatus(JobExecutionStatus.PENDING);
                        jobExecution.setScheduledTime(java.time.Instant.now());
                        jobExecutionRepository.save(jobExecution);
                    }

                    default -> {
                        jobExecution.setStatus(JobExecutionStatus.FAILED);
                        jobExecutionRepository.save(jobExecution);
                        throw new IllegalArgumentException("No misfire policy defined for job: " + jobExecution.getJobId().getName());
                    }
                }
                log.info("Misfire executed {} for job execution {} - {}", jobExecution.getJobId().getMisfirePolicy(), jobExecution.getJobId().getName(), jobExecution.getId());
            }
        });
    }
}
