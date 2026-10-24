package com.personal.distributedtaskscheduler.service;

public record AuditLogService(String tenantId, String actor, String action, String entityId, String details) {
}
