package com.personal.distributedtaskscheduler.repository;

import com.personal.distributedtaskscheduler.entity.JobExecution;
import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
    List<JobExecution> findAllByStatus(JobExecutionStatus jobExecutionStatus);

    @Query("select je from job_executions je where je.job.id = :jobId")
    List<JobExecution> findAllByJobId(UUID jobId);

    List<JobExecution> findJobExecutionsByStatus(JobExecutionStatus status);

    List<JobExecution> findJobExecutionsByStatusIn(Collection<JobExecutionStatus> statuses);
}
