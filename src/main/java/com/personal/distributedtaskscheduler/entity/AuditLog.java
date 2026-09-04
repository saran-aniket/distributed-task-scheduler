package com.personal.distributedtaskscheduler.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity(name = "audit_log")
public class AuditLog extends BaseModel{
    @ManyToOne
    @JoinColumn(name = "tenant_id", columnDefinition = "uuid")
    private Tenant tenant;

    @Column(name = "actor", columnDefinition = "varchar(255)")
    private String actor;

    @Column(name = "action", columnDefinition = "varchar(100)")
    private String action;

    @Column(name = "entity_id", columnDefinition = "uuid")
    private UUID entityId;

    @JdbcTypeCode(value = SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;
}
