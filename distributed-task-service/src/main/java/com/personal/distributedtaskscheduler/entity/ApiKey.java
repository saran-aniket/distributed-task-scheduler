package com.personal.distributedtaskscheduler.entity;

import com.personal.distributedtaskscheduler.entity.enums.RoleType;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Entity(name = "api_keys")
public class ApiKey extends BaseModel{
    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false, columnDefinition = "uuid")
    private Tenant tenant;

    @Column(name = "key_hash", columnDefinition = "varchar(255)")
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", columnDefinition = "varchar(20)")
    private RoleType role;

    @DateTimeFormat
    @Column(name = "revoked_at", columnDefinition = "timestamp")
    private Instant revokedAt;

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
