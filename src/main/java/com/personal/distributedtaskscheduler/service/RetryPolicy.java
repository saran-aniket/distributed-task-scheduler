package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(RetryPolicy.class);
    private final JobExecutionRepository jobExecutionRepository;

    public RetryPolicy(JobExecutionRepository jobExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
    }

    @Scheduled(fixedRate = 10000)
    public void retryFailedJobs(){
        List<JobExecution> failedJobs = jobExecutionRepository.findJobExecutionsByStatus(JobExecutionStatus.FAILED);
        for (JobExecution jobExecution : failedJobs) {
            if (jobExecution.getAttemptNumber() < jobExecution.getJobId().getMaxRetries()) {
                log.info("Retrying job execution {} - {}", jobExecution.getJobId().getName(), jobExecution.getId());
                processRetry(jobExecution);
            }
        }
    }

    private void processRetry(JobExecution jobExecution){
        Job job = jobExecution.getJobId();

        int backOffMultiplier = (int) Math.pow(2, jobExecution.getAttemptNumber()-1);
        int totalWaitTime = backOffMultiplier * job.getBackoffSeconds();

        JobExecution retryJobExecution = new JobExecution();
        retryJobExecution.setJobId(job);
        retryJobExecution.setStatus(JobExecutionStatus.PENDING);
        retryJobExecution.setScheduledTime(Instant.now().plusSeconds(totalWaitTime));
        retryJobExecution.setAttemptNumber(jobExecution.getAttemptNumber() + 1);
        jobExecutionRepository.save(retryJobExecution);

        log.info("new job attempt queued {} - {}", jobExecution.getJobId().getName(), retryJobExecution.getId());
    }
}
