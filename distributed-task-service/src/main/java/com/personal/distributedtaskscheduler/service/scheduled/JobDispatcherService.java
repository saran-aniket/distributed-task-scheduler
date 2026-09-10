package com.personal.distributedtaskscheduler.service.scheduled;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;
import com.personal.distributedtaskscheduler.factory.JobExecutorFactory;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.service.DistributedLockService;
import com.personal.distributedtaskscheduler.service.RetryPolicy;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class JobDispatcherService {
    private static final Logger log = LoggerFactory.getLogger(JobDispatcherService.class);
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutorFactory jobExecutorFactory;
    private final DistributedLockService distributedLockService;
    private final RetryPolicy retryPolicy;

    @Value("${scheduler.node-id:${spring.application.name}}")
    private String nodeId;

    @Value("${scheduler.wait-time-ms}")
    private int waitTimeMillis;

    @Value("${scheduler.lease-time-ms}")
    private int leaseTimeMillis;

    public JobDispatcherService(
            JobExecutionRepository jobExecutionRepository,
            JobExecutorFactory jobExecutorFactory,
            DistributedLockService distributedLockService,
            RetryPolicy retryPolicy
    ) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutorFactory = jobExecutorFactory;
        this.distributedLockService = distributedLockService;
        this.retryPolicy = retryPolicy;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${scheduler.dispatch-interval-ms}")
    public void dispatchJobs() {
        log.info("Dispatching jobs...");
        List<JobExecution> pendingExecutions = jobExecutionRepository.findAllByStatus(JobExecutionStatus.PENDING);

        log.info("Jobs dispatched :{}", pendingExecutions.size());
        //Claiming the job
        for(JobExecution jobExecution : pendingExecutions){
            String lockKey = String.valueOf(jobExecution.getId());
            Optional<RLock> lock = distributedLockService.tryLock(lockKey, Duration.ofMillis(waitTimeMillis), Duration.ofMillis(leaseTimeMillis));
            if(lock.isEmpty()){
                log.warn("Could not claim job execution {}", jobExecution.getId());
                continue;
            }

            try {
                if (!markExecutionAsClaimed(jobExecution)) {
                    log.warn("Execution {} was already claimed before node {} could persist claim", jobExecution.getId(), nodeId);
                    continue;
                }
                markExecutionAsRunning(jobExecution);

                ExecutionResult executionResult = jobExecutorFactory.getJobExecutor(jobExecution.getJobId().getJobType()).execute(jobExecution, jobExecution.getJobId());
                if(executionResult.isSuccess()){
                    jobExecution.setStatus(JobExecutionStatus.SUCCESS);
                    jobExecution.setCompletedAt(Instant.now());
                    jobExecutionRepository.save(jobExecution);
                }else{
                    jobExecution.setErrorMessage(executionResult.getMessage());
                    if(retryPolicy.shouldRetry(jobExecution.getAttemptNumber(), jobExecution.getJobId().getMaxRetries())){
                        processRetry(jobExecution);
                    }else{
                        jobExecution.setStatus(JobExecutionStatus.FAILED);
                        jobExecution.setCompletedAt(Instant.now());
                        jobExecutionRepository.save(jobExecution);
                    }
                }
            } finally {
                distributedLockService.unlock(lockKey);
            }
        }
    }

    protected boolean markExecutionAsClaimed(JobExecution jobExecution){
        int updated = jobExecutionRepository.claimPendingExecution(
                jobExecution.getId(),
                JobExecutionStatus.PENDING,
                JobExecutionStatus.CLAIMED,
                nodeId
        );
        if (updated == 1) {
            jobExecution.setStatus(JobExecutionStatus.CLAIMED);
            jobExecution.setClaimedByNode(nodeId);
            return true;
        }
        return false;
    }

    private void markExecutionAsRunning(JobExecution jobExecution){
        jobExecution.setStartedAt(Instant.now());
        jobExecution.setStatus(JobExecutionStatus.RUNNING);
        jobExecutionRepository.save(jobExecution);
    }

    private void processRetry(JobExecution jobExecution){
        log.info("Job execution failed. Retrying job {}", jobExecution.getId());
        Job job = jobExecution.getJobId();
        int totalWaitTime = retryPolicy.calculateBackoffSeconds(jobExecution.getAttemptNumber(), job.getBackoffSeconds());

        JobExecution retryJobExecution = new JobExecution();
        retryJobExecution.setJobId(job);
        retryJobExecution.setStatus(JobExecutionStatus.PENDING);
        retryJobExecution.setScheduledTime(Instant.now().plusSeconds(totalWaitTime));
        retryJobExecution.setAttemptNumber(jobExecution.getAttemptNumber() + 1);
        jobExecutionRepository.save(retryJobExecution);

        log.info("new job attempt queued {} - {}", jobExecution.getJobId().getName(), retryJobExecution.getId());
    }
}
