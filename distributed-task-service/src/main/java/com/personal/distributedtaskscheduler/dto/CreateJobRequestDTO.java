package com.personal.distributedtaskscheduler.dto;

import com.personal.distributedtaskscheduler.validation.CronExpressions;
import jakarta.validation.constraints.*;

public class CreateJobRequestDTO {
    @NotBlank( message = "Name cannot be blank")
    private String name;

    // Nullable for one-off jobs; when present it must be a valid 5-field cron expression.
    @Pattern(regexp = CronExpressions.CRON_REGEX, message = "Invalid cron expression; expected 5 whitespace-separated fields")
    private String cronExpression;

    @NotNull( message = "Job Type cannot be null")
    private String jobType;

    @Size(max = 10000)
    private String payLoad;

    private String webhookUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(String payLoad) {
        this.payLoad = payLoad;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
