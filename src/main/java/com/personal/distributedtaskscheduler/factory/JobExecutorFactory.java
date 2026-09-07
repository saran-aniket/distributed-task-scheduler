package com.personal.distributedtaskscheduler.factory;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobType;
import com.personal.distributedtaskscheduler.executor.JobExecutor;
import com.personal.distributedtaskscheduler.executor.executorImpl.HTTPCallbackExecutor;
import com.personal.distributedtaskscheduler.executor.executorImpl.SimulatedJobExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JobExecutorFactory {

    private final Map<JobType, JobExecutor> jobExecutorMap;

    public JobExecutorFactory(List<JobExecutor> jobExecutors) {
        this.jobExecutorMap = jobExecutors.stream()
                .collect(Collectors.toMap(JobExecutor::getJobType, Function.identity()));
    }

    public JobExecutor getJobExecutor(JobType jobType) {
        JobExecutor jobExecutor = jobExecutorMap.get(jobType);
        if (jobExecutor == null) {
            throw new IllegalArgumentException("Unsupported job type: " + jobType);
        }
        return jobExecutor;
    }
}
