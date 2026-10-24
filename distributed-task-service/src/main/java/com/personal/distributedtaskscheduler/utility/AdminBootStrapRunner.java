package com.personal.distributedtaskscheduler.utility;

import com.personal.distributedtaskscheduler.entity.ApiKey;
import com.personal.distributedtaskscheduler.entity.Tenant;
import com.personal.distributedtaskscheduler.entity.enums.RoleType;
import com.personal.distributedtaskscheduler.repository.ApiKeyRepository;
import com.personal.distributedtaskscheduler.repository.TenantRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootStrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootStrapRunner.class);

    private final ApiKeyRepository apiKeyRepository;
    private final SecureKeyGenerator secureKeyGenerator;
    private final TenantRepository tenantRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public AdminBootStrapRunner(ApiKeyRepository apiKeyRepository, SecureKeyGenerator secureKeyGenerator, TenantRepository tenantRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.apiKeyRepository = apiKeyRepository;
        this.secureKeyGenerator = secureKeyGenerator;
        this.tenantRepository = tenantRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public void run(@NonNull ApplicationArguments args) {
        if (apiKeyRepository.countByRole(RoleType.ADMIN) == 0) {
            String plaintextKey = secureKeyGenerator.generateSecureKey(32);
            String keyPrefix = secureKeyGenerator.generateSecureKey(8);
            Tenant tenant = new Tenant();
            tenant.setFirstName("SYSTEM");
            tenant.setLastName("ADMINISTRATOR");
            tenant.setEmail("");
            tenantRepository.save(tenant);

            ApiKey apiKey = new ApiKey();
            apiKey.setTenant(tenant);
            apiKey.setRole(RoleType.ADMIN);
            apiKey.setKeyPrefix(keyPrefix);
            apiKey.setKeyHash(bCryptPasswordEncoder.encode(plaintextKey));
            apiKeyRepository.save(apiKey);
            log.warn("No ADMIN key found — bootstrapped one: {}", plaintextKey);
            log.warn("Store this now — it will not be shown again.");
        }
    }
}