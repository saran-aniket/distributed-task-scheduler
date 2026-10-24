package com.personal.distributedtaskscheduler.utility;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureKeyGenerator {
    SecureRandom secureRandom = new SecureRandom();
    public String generateSecureKey(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }
}
