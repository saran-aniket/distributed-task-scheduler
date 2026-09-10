package com.personal.distributedtaskscheduler.dto;

import com.personal.distributedtaskscheduler.entity.Job;

public class JobResponseDTO {
    private String id;

    private String name;

    private String cronExpression;

    private String jobType;

    private String payLoad;

    private String webhookUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public static JobResponseDTO from(Job job){
        JobResponseDTO jobResponseDTO = new JobResponseDTO();
        jobResponseDTO.setId(job.getId().toString());
        jobResponseDTO.setName(job.getName());
        jobResponseDTO.setJobType(job.getJobType().name());
        jobResponseDTO.setCronExpression(job.getCronExpression());
        jobResponseDTO.setPayLoad(job.getPayload().toString());
        jobResponseDTO.setWebhookUrl(job.getWebhookUrl());
        return jobResponseDTO;
    }
}
