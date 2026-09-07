package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.executor.JobExecutor;
import com.personal.distributedtaskscheduler.factory.JobExecutorFactory;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobDispatcherService {
    private static final Logger log = LoggerFactory.getLogger(JobDispatcherService.class);
    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutorFactory jobExecutorFactory;

    public JobDispatcherService(JobExecutionRepository jobExecutionRepository, JobExecutorFactory jobExecutorFactory) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutorFactory = jobExecutorFactory;
    }

    @Scheduled(fixedDelayString = "${scheduler.dispatch-interval-ms}")
    public void dispatchJobs() {
        log.info("Dispatching jobs...");
        List<JobExecution> pendingExecutions = jobExecutionRepository.findAllByStatus(JobExecutionStatus.PENDING);

        log.info("Jobs dispatched :{}", pendingExecutions.size());
        //Claiming the job
        for(JobExecution jobExecution : pendingExecutions){
            jobExecution.setStatus(JobExecutionStatus.CLAIMED);
            jobExecutionRepository.save(jobExecution);
        }

        //Running the job
        for(JobExecution jobExecution : pendingExecutions){
            jobExecution.setStatus(JobExecutionStatus.RUNNING);
            jobExecutionRepository.save(jobExecution);

            jobExecutorFactory.getJobExecutor(jobExecution.getJobId().getJobType()).execute(jobExecution, jobExecution.getJobId());
        }
    }
}
