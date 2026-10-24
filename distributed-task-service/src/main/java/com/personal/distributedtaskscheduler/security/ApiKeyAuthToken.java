package com.personal.distributedtaskscheduler.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class ApiKeyAuthToken extends AbstractAuthenticationToken{
    private final Object principal;
    private final String credentials;
    private final String keyPrefix;

    public ApiKeyAuthToken(String apiKey) {
        super((Collection<? extends GrantedAuthority>) null);
        this.credentials = apiKey;
        this.principal = null;
        this.keyPrefix = null;
        setAuthenticated(false);
    }

    public ApiKeyAuthToken(Object principal, Collection<? extends GrantedAuthority> authorities, String keyPrefix) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        this.keyPrefix = keyPrefix;
        setAuthenticated(true);
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }
}
