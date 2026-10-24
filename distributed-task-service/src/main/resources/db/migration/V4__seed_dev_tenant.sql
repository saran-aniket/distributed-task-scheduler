-- Seed a temporary dev tenant and API key for local development.
-- Fixed UUIDs are used so the data is idempotent and easy to reference.

INSERT INTO tenants (id, first_name, last_name, email, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Dev',
    'Tenant',
    'dev@example.com',
    now()
)
ON CONFLICT (id) DO NOTHING;

-- A non-null prefix is required for the API key lookup path used by the auth filter.
INSERT INTO api_keys (id, tenant_id, key_prefix, key_hash, role, created_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'devkey',
    '$2a$12$BXtLIegEyMKoFryswjrHIuuaG3nIU6aq3FJSJoRfx3W7JQEkECLmW',
    'ADMIN',
    now()
)
ON CONFLICT (id) DO NOTHING;
