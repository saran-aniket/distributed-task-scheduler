package com.personal.distributedtaskscheduler.service.scheduled;

import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ScheduledExecutorService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledExecutorService.class);
    private final JobExecutionRepository jobExecutionRepository;
    @Value("${spring.redisson-lock-prefix:distributed_lock_}")
    private String LOCK_PREFIX;
    private final RedissonClient redissonClient;

    public ScheduledExecutorService(JobExecutionRepository jobExecutionRepository, RedissonClient redissonClient) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.redissonClient = redissonClient;
    }

    @Scheduled(fixedRate = 10000)
    public void renewTTLForRunningJobs(){
        List<JobExecution> runningJobExecutions = jobExecutionRepository.findJobExecutionsByStatus(JobExecutionStatus.RUNNING);

        for(JobExecution jobExecution : runningJobExecutions){
            RLock lock = redissonClient.getLock(LOCK_PREFIX + jobExecution.getId());

            try{
                if(lock.isLocked()){
                    log.info("Renewing TTL for job execution {}", jobExecution.getId());
                    redissonClient.getBucket(LOCK_PREFIX + jobExecution.getId()).expire(Duration.ofSeconds(30)); // Renew the lock for another 30 seconds
                }
            }catch (Exception e){
                log.error("Error renewing TTL for job execution {}", jobExecution.getId(), e);
            }
        }
    }
}
