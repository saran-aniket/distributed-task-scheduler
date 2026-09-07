package com.personal.distributedtaskscheduler.executor;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobType;

public interface JobExecutor {
    JobExecutionStatus execute(JobExecution jobExecution, Job job);
    JobType getJobType();
}
