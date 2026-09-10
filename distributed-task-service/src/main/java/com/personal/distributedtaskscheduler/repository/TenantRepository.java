package com.personal.distributedtaskscheduler.repository;

import com.personal.distributedtaskscheduler.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
