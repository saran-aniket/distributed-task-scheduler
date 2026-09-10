package com.personal.distributedtaskscheduler.executor.model;

public class ExecutionResult {
    private String message;
    private boolean success;

    public ExecutionResult(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }
}
