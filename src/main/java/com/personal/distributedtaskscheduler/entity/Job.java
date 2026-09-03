package com.personal.distributedtaskscheduler.entity;

import com.personal.distributedtaskscheduler.entity.enums.JobMisfirePolicy;
import com.personal.distributedtaskscheduler.entity.enums.JobStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity(name = "jobs")
public class Job extends BaseModel{
    @ManyToOne
    @JoinColumn(name = "tenant_id", columnDefinition = "uuid")
    private Tenant tenant;

    @Column(name = "name", columnDefinition = "varchar(255)")
    private String name;

    @Column(name = "job_type", columnDefinition = "varchar(50)")
    private String jobType;

    @Column(name = "cron_expression", columnDefinition = "varchar(100)")
    private String cronExpression;

    @JdbcTypeCode(value = SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated
    @Column(name = "status", columnDefinition = "varchar(20)")
    private JobStatus status;

    @Column(name = "webhook_url", columnDefinition = "text")
    private String webhookUrl;

    @Column(name = "max_retries", columnDefinition = "integer")
    private int maxRetries;

    @Column(name = "backoff_seconds", columnDefinition = "integer")
    private int backoffSeconds;

    @Enumerated
    @Column(name = "misfire_policy", columnDefinition = "varchar(20)")
    private JobMisfirePolicy misfirePolicy;

    @Column(name = "next_fire_time", columnDefinition = "timestamp")
    private Instant nextFireTime;

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getBackoffSeconds() {
        return backoffSeconds;
    }

    public void setBackoffSeconds(int backoffSeconds) {
        this.backoffSeconds = backoffSeconds;
    }

    public JobMisfirePolicy getMisfirePolicy() {
        return misfirePolicy;
    }

    public void setMisfirePolicy(JobMisfirePolicy misfirePolicy) {
        this.misfirePolicy = misfirePolicy;
    }

    public Instant getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Instant nextFireTime) {
        this.nextFireTime = nextFireTime;
    }
}
