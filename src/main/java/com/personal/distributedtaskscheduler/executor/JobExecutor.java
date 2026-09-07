package com.personal.distributedtaskscheduler.executor;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.executor.model.ExecutionResult;

public interface JobExecutor {
    ExecutionResult execute(JobExecution jobExecution, Job job);
    JobType getJobType();
}
