package com.personal.distributedtaskscheduler.service.scheduled;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.utility.CronExpressionParser;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobScannerService {
    private static final Logger log = LoggerFactory.getLogger(JobScannerService.class);
    private final CronExpressionParser cronExpressionParser;
    private final JobRepository jobRepository;
    private final JobExecutionRepository jobExecutionRepository;
    @Value("${scheduler.batch-size}")
    private final int batchSize = 10;

    public JobScannerService(CronExpressionParser cronExpressionParser, JobRepository jobRepository, JobExecutionRepository jobExecutionRepository) {
        this.cronExpressionParser = cronExpressionParser;
        this.jobRepository = jobRepository;
        this.jobExecutionRepository = jobExecutionRepository;
    }

    @Transactional(rollbackOn = Exception.class)
    public void scanJobs(){
        List<Job> activeJobs = jobRepository.findByStatusAndNextFireTimeLessThanEqual(JobStatus.ACTIVE.name(), java.time.Instant.now(), batchSize);
        log.info("Found {} active jobs to process", activeJobs.size());
        for(Job job : activeJobs){
            JobExecution jobExecution = new JobExecution();
            jobExecution.setJobId(job);
            jobExecution.setStatus(JobExecutionStatus.PENDING);
            jobExecution.setScheduledTime(job.getNextFireTime());
            jobExecution.setAttemptNumber(1);
            jobExecutionRepository.save(jobExecution);

            job.setNextFireTime(cronExpressionParser.nextFireTime(job.getCronExpression(), job.getNextFireTime()));
        }
    }
}
