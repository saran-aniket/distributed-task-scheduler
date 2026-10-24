package com.personal.distributedtaskscheduler.dto;

import com.personal.distributedtaskscheduler.entity.ApiKey;

public class ApiKeyResponseDto {
    public String apiKey;

    public ApiKeyResponseDto(String apiKey) {
        this.apiKey = apiKey;
    }
}
