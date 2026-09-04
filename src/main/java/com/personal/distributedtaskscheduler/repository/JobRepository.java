package com.personal.distributedtaskscheduler.repository;

import com.personal.distributedtaskscheduler.entity.Job;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByStatusAndNextFireTimeLessThanEqual(JobStatus status, Instant now);
}
