package com.personal.distributedtaskscheduler.service.scheduled;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;
import com.personal.distributedtaskscheduler.factory.JobExecutorFactory;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.service.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JobDispatcherService {
    private static final Logger log = LoggerFactory.getLogger(JobDispatcherService.class);
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutorFactory jobExecutorFactory;
    private final DistributedLockService distributedLockService;

    @Value("${scheduler.wait-time-ms}")
    private int waitTimeMillis;

    @Value("${scheduler.lease-time-ms}")
    private int leaseTimeMillis;

    public JobDispatcherService(JobExecutionRepository jobExecutionRepository, JobExecutorFactory jobExecutorFactory, DistributedLockService distributedLockService) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutorFactory = jobExecutorFactory;
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(fixedDelayString = "${scheduler.dispatch-interval-ms}")
    public void dispatchJobs() {
        log.info("Dispatching jobs...");
        List<JobExecution> pendingExecutions = jobExecutionRepository.findAllByStatus(JobExecutionStatus.PENDING);

        log.info("Jobs dispatched :{}", pendingExecutions.size());
        //Claiming the job
        for(JobExecution jobExecution : pendingExecutions){
            boolean isLockAcquired = distributedLockService.tryExecuteWithLock(String.valueOf(jobExecution.getId()), Duration.ofMillis(waitTimeMillis), Duration.ofMillis(leaseTimeMillis));
            if(!isLockAcquired){
                log.warn("Could not claim job execution {}", jobExecution.getId());
                return;
            }

            markExecutionAsClaimed(jobExecution);
            markExecutionAsRunning(jobExecution);

            ExecutionResult executionResult = jobExecutorFactory.getJobExecutor(jobExecution.getJobId().getJobType()).execute(jobExecution, jobExecution.getJobId());
            if(executionResult.isSuccess()){
                jobExecution.setStatus(JobExecutionStatus.SUCCESS);
                jobExecution.setCompletedAt(Instant.now());
                jobExecutionRepository.save(jobExecution);
            }else{
                jobExecution.setStatus(JobExecutionStatus.FAILED);
                jobExecutionRepository.save(jobExecution);
            }
        }
    }

    private void markExecutionAsClaimed(JobExecution jobExecution){
        jobExecution.setStatus(JobExecutionStatus.CLAIMED);
        jobExecutionRepository.save(jobExecution);
    }

    private void markExecutionAsRunning(JobExecution jobExecution){
        jobExecution.setStartedAt(Instant.now());
        jobExecution.setStatus(JobExecutionStatus.RUNNING);
        jobExecutionRepository.save(jobExecution);
    }
}
