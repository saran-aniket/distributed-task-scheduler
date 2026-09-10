package com.personal.distributedtaskscheduler.service.scheduled;

import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.service.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ScheduledExecutorService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledExecutorService.class);
    private final JobExecutionRepository jobExecutionRepository;
    private final DistributedLockService distributedLockService;

    @Value("${scheduler.lease-time-ms:30000}")
    private int leaseTimeMillis;

    public ScheduledExecutorService(JobExecutionRepository jobExecutionRepository, DistributedLockService distributedLockService) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(fixedRate = 10000)
    public void renewTTLForRunningJobs(){
        List<JobExecution> runningJobExecutions = jobExecutionRepository.findJobExecutionsByStatus(JobExecutionStatus.RUNNING);

        for(JobExecution jobExecution : runningJobExecutions){
            try{
                log.info("Renewing TTL for job execution {}", jobExecution.getId());
                distributedLockService.renewLock(String.valueOf(jobExecution.getId()), Duration.ofMillis(leaseTimeMillis));
            }catch (Exception e){
                log.error("Error renewing TTL for job execution {}", jobExecution.getId(), e);
            }
        }
    }
}
