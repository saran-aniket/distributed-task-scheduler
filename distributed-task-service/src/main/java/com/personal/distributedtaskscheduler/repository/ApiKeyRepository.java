package com.personal.distributedtaskscheduler.repository;

import com.personal.distributedtaskscheduler.entity.ApiKey;
import com.personal.distributedtaskscheduler.entity.Tenant;
import com.personal.distributedtaskscheduler.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findApiKeyByTenant(Tenant tenant);

    double countByRole(RoleType role);

    ApiKey findApiKeyByKeyPrefix(String keyPrefix);
}
