package com.personal.distributedtaskscheduler.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public class BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", columnDefinition = "timestamp")
    private Instant createdDate;

    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "timestamp")
    private Instant lastModifiedDate;
}
