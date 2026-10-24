package com.personal.distributedtaskscheduler.controller;

import com.personal.distributedtaskscheduler.dto.ApiKeyRequestDto;
import com.personal.distributedtaskscheduler.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/keys")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getKeys(@RequestBody ApiKeyRequestDto apiKeyRequest) {
        String apiKey = authService.createTenantApiKey(apiKeyRequest.getTenantId(), apiKeyRequest.getRole());
        return ResponseEntity.ok(Map.of("apiKey", apiKey));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        String jwtToken = authService.authenticateAndGenerateToken(apiKey);
        return ResponseEntity.ok(Map.of(
                "token", jwtToken,
                "tokenType", "Bearer",
                "expiresIn", 300
        ));
    }
}
