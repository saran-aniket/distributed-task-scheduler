package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import com.personal.distributedtaskscheduler.repository.JobExecutionRepository;
import com.personal.distributedtaskscheduler.repository.JobRepository;
import com.personal.distributedtaskscheduler.service.scheduled.JobScannerService;
import com.personal.distributedtaskscheduler.utility.CronExpressionParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobScannerServiceTest {

    @Mock
    private CronExpressionParser cronExpressionParser;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @InjectMocks
    private JobScannerService jobScannerService;

    @Test
    void scanJobs_createsExecutionsForDueActiveJobsAndAdvancesNextFireTime() {
        Job dueJobOne = activeJob("due-job-one", "0 * * * * *", Instant.parse("2026-09-05T10:00:00Z"));
        Job dueJobTwo = activeJob("due-job-two", "0 */5 * * * *", Instant.parse("2026-09-05T10:05:00Z"));
        Job notDueJob = activeJob("not-due-job", "0 0 14 * * *", Instant.parse("2026-09-05T14:00:00Z"));
        Job pausedJob = pausedJob("paused-job", "0 30 9 * * 1,3,5", Instant.parse("2026-09-05T09:30:00Z"));

        Instant dueJobOneNext = Instant.parse("2026-09-05T10:01:00Z");
        Instant dueJobTwoNext = Instant.parse("2026-09-05T10:10:00Z");

        when(jobRepository.findByStatusAndNextFireTimeLessThanEqual(eq(JobStatus.ACTIVE.name()), any(Instant.class), anyInt()))
                .thenReturn(List.of(dueJobOne, dueJobTwo));
        when(cronExpressionParser.nextFireTime(dueJobOne.getCronExpression(), dueJobOne.getNextFireTime()))
                .thenReturn(dueJobOneNext);
        when(cronExpressionParser.nextFireTime(dueJobTwo.getCronExpression(), dueJobTwo.getNextFireTime()))
                .thenReturn(dueJobTwoNext);

        jobScannerService.scanJobs();

        ArgumentCaptor<JobExecution> executionCaptor = ArgumentCaptor.forClass(JobExecution.class);
        verify(jobExecutionRepository, times(2)).save(executionCaptor.capture());

        List<JobExecution> executions = executionCaptor.getAllValues();
        assertThat(executions).hasSize(2);

        JobExecution firstExecution = executions.get(0);
        assertThat(firstExecution.getJobId()).isSameAs(dueJobOne);
        assertThat(firstExecution.getStatus()).isEqualTo(JobExecutionStatus.PENDING);
        assertThat(firstExecution.getScheduledTime()).isEqualTo(Instant.parse("2026-09-05T10:00:00Z"));
        assertThat(firstExecution.getAttemptNumber()).isEqualTo(1);

        JobExecution secondExecution = executions.get(1);
        assertThat(secondExecution.getJobId()).isSameAs(dueJobTwo);
        assertThat(secondExecution.getStatus()).isEqualTo(JobExecutionStatus.PENDING);
        assertThat(secondExecution.getScheduledTime()).isEqualTo(Instant.parse("2026-09-05T10:05:00Z"));
        assertThat(secondExecution.getAttemptNumber()).isEqualTo(1);

        assertThat(dueJobOne.getNextFireTime()).isEqualTo(dueJobOneNext);
        assertThat(dueJobTwo.getNextFireTime()).isEqualTo(dueJobTwoNext);
        assertThat(notDueJob.getNextFireTime()).isEqualTo(Instant.parse("2026-09-05T14:00:00Z"));
        assertThat(pausedJob.getNextFireTime()).isEqualTo(Instant.parse("2026-09-05T09:30:00Z"));

        verify(cronExpressionParser).nextFireTime(dueJobOne.getCronExpression(), Instant.parse("2026-09-05T10:00:00Z"));
        verify(cronExpressionParser).nextFireTime(dueJobTwo.getCronExpression(), Instant.parse("2026-09-05T10:05:00Z"));
        verify(cronExpressionParser, never()).nextFireTime(notDueJob.getCronExpression(), notDueJob.getNextFireTime());
        verify(cronExpressionParser, never()).nextFireTime(pausedJob.getCronExpression(), pausedJob.getNextFireTime());
    }

    private Job activeJob(String name, String cronExpression, Instant nextFireTime) {
        Job job = new Job();
        job.setName(name);
        job.setStatus(JobStatus.ACTIVE);
        job.setCronExpression(cronExpression);
        job.setNextFireTime(nextFireTime);
        return job;
    }

    private Job pausedJob(String name, String cronExpression, Instant nextFireTime) {
        Job job = new Job();
        job.setName(name);
        job.setStatus(JobStatus.PAUSED);
        job.setCronExpression(cronExpression);
        job.setNextFireTime(nextFireTime);
        return job;
    }
}
