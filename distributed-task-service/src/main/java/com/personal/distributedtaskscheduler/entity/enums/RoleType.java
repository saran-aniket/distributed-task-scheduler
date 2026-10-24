package com.personal.distributedtaskscheduler.entity.enums;

public enum RoleType {
    ADMIN, USER, VIEWER;

    public static boolean isValidRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        try {
            RoleType.valueOf(role.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
