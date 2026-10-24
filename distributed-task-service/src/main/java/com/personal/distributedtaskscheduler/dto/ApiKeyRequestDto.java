package com.personal.distributedtaskscheduler.dto;

public class ApiKeyRequestDto {
    private String tenantId;
    private String role;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
