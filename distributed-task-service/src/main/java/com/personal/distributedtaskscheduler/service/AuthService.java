package com.personal.distributedtaskscheduler.service;

import com.personal.distributedtaskscheduler.entity.ApiKey;
import com.personal.distributedtaskscheduler.entity.Tenant;
import com.personal.distributedtaskscheduler.entity.enums.RoleType;
import com.personal.distributedtaskscheduler.repository.ApiKeyRepository;
import com.personal.distributedtaskscheduler.repository.TenantRepository;
import com.personal.distributedtaskscheduler.security.JwtProvider;
import com.personal.distributedtaskscheduler.utility.SecureKeyGenerator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final SecureKeyGenerator secureKeyGenerator;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(ApiKeyRepository apiKeyRepository,
                       TenantRepository tenantRepository,
                       SecureKeyGenerator secureKeyGenerator,
                       BCryptPasswordEncoder bCryptPasswordEncoder,
                       JwtProvider jwtProvider) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.secureKeyGenerator = secureKeyGenerator;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public String createTenantApiKey(String tenantId, String role) {
        Tenant tenant = tenantRepository.findById(UUID.fromString(tenantId))
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        if (!RoleType.isValidRole(role)) {
            throw new RuntimeException("Invalid role: " + role);
        }

        String keyPrefix = sanitizeTokenPart(secureKeyGenerator.generateSecureKey(12));
        String secretKey = sanitizeTokenPart(secureKeyGenerator.generateSecureKey(32));
        String hashedKey = bCryptPasswordEncoder.encode(secretKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(hashedKey);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setTenant(tenant);
        apiKey.setRole(RoleType.valueOf(role.toUpperCase()));
        apiKeyRepository.save(apiKey);
        return keyPrefix + "." + secretKey;
    }

    public String authenticateAndGenerateToken(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || !apiKey.contains(".")) {
            throw new BadCredentialsException("Invalid API Key format");
        }

        String[] apiKeyParts = apiKey.split("\\.", 2);
        String prefix = apiKeyParts[0];
        String secret = apiKeyParts[1];

        ApiKey apiKeyEntity = apiKeyRepository.findApiKeyByKeyPrefix(prefix);
        if (apiKeyEntity == null) {
            throw new BadCredentialsException("Unauthorized: Invalid API Key");
        }

        if (!bCryptPasswordEncoder.matches(secret, apiKeyEntity.getKeyHash())) {
            throw new BadCredentialsException("Unauthorized: Invalid API Key");
        }

        return jwtProvider.generateToken(apiKeyEntity.getTenant().getId().toString(), apiKeyEntity.getRole().name());
    }

    private String sanitizeTokenPart(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "");
    }
}

