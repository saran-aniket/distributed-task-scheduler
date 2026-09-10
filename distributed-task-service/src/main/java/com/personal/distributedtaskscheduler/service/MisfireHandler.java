package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobMisfirePolicy;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class MisfireHandler {

    private static final Logger log = LoggerFactory.getLogger(MisfireHandler.class);
    private final RedissonClient redissonClient;
    @Value("${scheduler.execution-timeout-ms:15000}")
    private int executionTimeOut;
    @Value("${scheduler.node-id:${spring.application.name}}")
    private String nodeId;

    private final JobExecutionRepository jobExecutionRepository;

    public MisfireHandler(JobExecutionRepository jobExecutionRepository, RedissonClient redissonClient) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.redissonClient = redissonClient;
    }

    @Scheduled(fixedRateString = "${scheduler.misfire-check-interval-ms:15000}")
    public void acquireLock() {
        RLock lock = redissonClient.getLock("misfire-handler-lock");
        if(lock.isLocked()){
            log.info("Lock is already acquired by another instance, skipping misfire handling.");
            return;
        }
        try{
            if(lock.tryLock(0,25, TimeUnit.SECONDS)){
                try {
                    log.info("Acquired lock for misfire handling by the node {}.", nodeId);
                    handleMisfire();
                } finally {
                    lock.unlock();
                }
            } else {
                log.info("Could not acquire lock, skipping misfire handling.");
            }
        }catch (Exception e){
            log.error("Error acquiring lock for misfire handling: {}", e.getMessage(), e);
        }
    }

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
