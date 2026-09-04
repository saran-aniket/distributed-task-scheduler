package com.personal.distributedtaskscheduler.entity;

import com.personal.distributedtaskscheduler.entity.enums.JobExecutionStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity(name = "job_executions")
public class JobExecution extends BaseModel{
    @ManyToOne
    @JoinColumn(name = "job_id", columnDefinition = "uuid")
    private Job job;

    @Column(name = "scheduled_time", columnDefinition = "timestamp")
    private Instant scheduledTime;

    @Column(name = "started_at", columnDefinition = "timestamp")
    private Instant startedAt;

    @Column(name = "completed_at", columnDefinition = "timestamp")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(20)")
    private JobExecutionStatus status;

    @Column(name = "attempt_number", columnDefinition = "integer")
    private int attemptNumber;

    @Column(name = "claimed_by_node", columnDefinition = "varchar(100)")
    private String claimedByNode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public Job getJobId() {
        return job;
    }

    public void setJobId(Job job) {
        this.job = job;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public JobExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(JobExecutionStatus status) {
        this.status = status;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getClaimedByNode() {
        return claimedByNode;
    }

    public void setClaimedByNode(String claimedByNode) {
        this.claimedByNode = claimedByNode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
