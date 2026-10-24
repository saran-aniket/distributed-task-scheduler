-- ============================================================================
-- TEST DATA INITIALIZATION SCRIPT
-- ============================================================================
-- This script runs ONLY in test environments and loads test data sets
-- It uses Flyway placeholders to allow configuration
--
-- Usage:
--   Configure in application-test.yaml:
--   spring:
--     flyway:
--       placeholders:
--         test.enabled: 'true'
--         test.tenant.count: '3'
--         test.jobs.per.tenant: '5'
--         test.executions.per.job: '10'
-- ============================================================================

-- Only execute if test.enabled is true (controlled by application profile)
-- This prevents accidental test data in production/dev environments

BEGIN;

-- ============================================================================
-- SECTION 1: TEST TENANTS
-- ============================================================================

-- Scenario: Three tenants with different characteristics
INSERT INTO tenants (id, first_name, last_name, email, created_at, updated_at) VALUES
                                                                                   ('550e8400-e29b-41d4-a716-446655440001'::uuid, 'Demo', 'SaaS-Company-1', '${test.user.prefix}company-1@test.com', now() - interval '90 days', now() - interval '30 days'),
                                                                                   ('550e8400-e29b-41d4-a716-446655440002'::uuid, 'Enterprise', 'Cloud-Platform', '${test.user.prefix}enterprise@test.com', now() - interval '60 days', now() - interval '15 days'),
                                                                                   ('550e8400-e29b-41d4-a716-446655440003'::uuid, 'Startup', 'Rapid-Growth-Co', '${test.user.prefix}startup@test.com', now() - interval '30 days', now() - interval '5 days');

-- ============================================================================
-- SECTION 2: TEST API KEYS
-- ============================================================================

-- Scenario: Multiple api keys per tenant with different roles
INSERT INTO api_keys (id, tenant_id, key_hash, role, created_at, revoked_at, updated_at) VALUES
                                                                                             -- SaaS Company 1 keys (all roles represented)
                                                                                             ('660e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '$2b$12$r9h8keZ.r9h8keZ.r9h8keZ.', 'ADMIN', now() - interval '30 days', NULL, now() - interval '30 days'),
                                                                                             ('660e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '$2b$12$s0i9lfA.s0i9lfA.s0i9lfA.', 'OWNER', now() - interval '20 days', NULL, now() - interval '20 days'),
                                                                                             ('660e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, '$2b$12$t1j0mgB.t1j0mgB.t1j0mgB.', 'VIEWER', now() - interval '10 days', NULL, now() - interval '10 days'),

                                                                                             -- Enterprise Platform keys (access control scenario)
                                                                                             ('660e8400-e29b-41d4-a716-446655440004'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, '$2b$12$u2k1nhC.u2k1nhC.u2k1nhC.', 'ADMIN', now() - interval '25 days', NULL, now() - interval '25 days'),
                                                                                             ('660e8400-e29b-41d4-a716-446655440005'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, '$2b$12$v3l2oiD.v3l2oiD.v3l2oiD.', 'OWNER', now() - interval '15 days', NULL, now() - interval '15 days'),
                                                                                             ('660e8400-e29b-41d4-a716-446655440006'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, '$2b$12$w4m3pjE.w4m3pjE.w4m3pjE.', 'VIEWER', now() - interval '5 days', NULL, now() - interval '5 days'),

                                                                                             -- Startup key (revoked credentials scenario)
                                                                                             ('660e8400-e29b-41d4-a716-446655440007'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, '$2b$12$x5n4qkF.x5n4qkF.x5n4qkF.', 'ADMIN', now() - interval '20 days', now() - interval '2 days', now() - interval '2 days'),
                                                                                             ('660e8400-e29b-41d4-a716-446655440008'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, '$2b$12$y6o5rlG.y6o5rlG.y6o5rlG.', 'OWNER', now() - interval '15 days', NULL, now() - interval '15 days');

-- ============================================================================
-- SECTION 3: TEST JOBS
-- ============================================================================

-- Scenario: Various job types and schedules for testing different scenarios
INSERT INTO jobs (id, tenant_id, name, job_type, cron_expression, payload, webhook_url, max_retries, backoff_seconds, misfire_policy, status, next_fire_time, created_at, updated_at) VALUES
                                                                                                                                                                                          -- SaaS Company 1: Active HTTP callback jobs
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'hourly-report-generation', 'HTTP_CALLBACK', '0 * * * *', '{"format":"pdf","recipient":"admin@company1.com"}', 'https://webhook.company1.com/reports', 3, 30, 'SKIP', 'ACTIVE', now() + interval '5 minutes', now() - interval '30 days', now()),
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'daily-data-sync', 'INTERNAL_TASK', '0 2 * * *', '{"source":"legacy-db","target":"new-system"}', NULL, 5, 60, 'FIRE_ONCE', 'ACTIVE', now() + interval '2 hours', now() - interval '25 days', now()),
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'weekly-backup', 'INTERNAL_TASK', '0 3 ? * SUN', '{"retention_days":30}', NULL, 2, 120, 'SKIP', 'ACTIVE', now() + interval '3 days', now() - interval '20 days', now()),
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440004'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'paused-notification-task', 'HTTP_CALLBACK', '*/30 * * * *', '{"type":"email"}', 'https://notify.company1.com/send', 1, 15, 'SKIP', 'PAUSED', now() + interval '1 day', now() - interval '15 days', now()),

                                                                                                                                                                                          -- Enterprise Platform: High-volume job processing
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440005'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'real-time-metric-collection', 'HTTP_CALLBACK', '* * * * *', '{"interval":"1m","metrics":["cpu","memory","disk"]}', 'https://metrics.enterprise.com/collect', 10, 5, 'FIRE_ONCE', 'ACTIVE', now() + interval '30 seconds', now() - interval '45 days', now()),
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440006'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'batch-user-provisioning', 'INTERNAL_TASK', '0 0 * * *', '{"batch_size":100,"create_trial_accounts":true}', NULL, 3, 45, 'SKIP', 'ACTIVE', now() + interval '8 hours', now() - interval '35 days', now()),
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440007'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'compliance-audit-log-export', 'HTTP_CALLBACK', '0 */6 * * *', '{"format":"csv","include_sensitive":false}', 'https://compliance.enterprise.com/export', 2, 90, 'SKIP', 'ACTIVE', now() + interval '4 hours', now() - interval '30 days', now()),

                                                                                                                                                                                          -- Startup: Recently created jobs with high retry settings
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440008'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'experimental-cache-warming', 'INTERNAL_TASK', '*/15 * * * *', '{"cache_size":"500MB","preload_patterns":["*"]}', NULL, 5, 20, 'FIRE_ONCE', 'ACTIVE', now() + interval '10 minutes', now() - interval '10 days', now()),
                                                                                                                                                                                          ('750e8400-e29b-41d4-a716-446655440009'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'unstable-external-api-call', 'HTTP_CALLBACK', '0 */2 * * *', '{"endpoint":"api.flaky-provider.com/process","timeout":30000}', 'https://startup.local/callback', 7, 10, 'FIRE_ONCE', 'ACTIVE', now() + interval '1 hour', now() - interval '8 days', now());

-- ============================================================================
-- SECTION 4: TEST JOB EXECUTIONS
-- ============================================================================

-- Scenario: Historical and recent execution records showing different states

-- SaaS Company 1 executions
INSERT INTO job_executions (id, job_id, scheduled_time, started_at, completed_at, status, attempt_number, claimed_by_node, error_message, created_at, updated_at) VALUES
                                                                                                                                                                      -- Successful executions
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440001'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '1 hour', now() - interval '59 minutes', now() - interval '58 minutes', 'SUCCESS', 1, 'node-0', NULL, now() - interval '1 hour', now() - interval '58 minutes'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440002'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '2 hours', now() - interval '119 minutes', now() - interval '118 minutes', 'SUCCESS', 1, 'node-1', NULL, now() - interval '2 hours', now() - interval '118 minutes'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440003'::uuid, '750e8400-e29b-41d4-a716-446655440002'::uuid, now() - interval '1 day', now() - interval '1 day' - interval '2 minutes', now() - interval '1 day' - interval '1 minute', 'SUCCESS', 1, 'node-0', NULL, now() - interval '1 day', now() - interval '1 day' - interval '1 minute'),

                                                                                                                                                                      -- Failed execution with retries
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440004'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '3 hours', now() - interval '179 minutes', NULL, 'FAILED', 1, 'node-2', 'Connection timeout: http://webhook.company1.com/reports', now() - interval '3 hours', now() - interval '3 hours'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440005'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '3 hours', now() - interval '175 minutes', NULL, 'FAILED', 2, 'node-1', 'Connection timeout: http://webhook.company1.com/reports', now() - interval '175 minutes', now() - interval '175 minutes'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440006'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '3 hours', now() - interval '171 minutes', now() - interval '170 minutes', 'SUCCESS', 3, 'node-0', NULL, now() - interval '171 minutes', now() - interval '170 minutes'),

                                                                                                                                                                      -- Pending execution
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440007'::uuid, '750e8400-e29b-41d4-a716-446655440002'::uuid, now() + interval '30 minutes', NULL, NULL, 'PENDING', 1, NULL, NULL, now(), now()),

                                                                                                                                                                      -- Claimed but not yet running
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440008'::uuid, '750e8400-e29b-41d4-a716-446655440003'::uuid, now() - interval '5 minutes', NULL, NULL, 'CLAIMED', 1, 'node-3', NULL, now() - interval '5 minutes', now() - interval '5 minutes'),

                                                                                                                                                                      -- Timeout execution
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440009'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '2 days', now() - interval '2 days' - interval '2 minutes', NULL, 'TIMED_OUT', 1, 'node-1', 'Execution exceeded timeout of 300000ms', now() - interval '2 days', now() - interval '2 days'),

                                                                                                                                                                      -- Running execution
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440010'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, now() - interval '2 minutes', now() - interval '1 minute', NULL, 'RUNNING', 1, 'node-0', NULL, now() - interval '2 minutes', now() - interval '1 minute');

-- Enterprise Platform executions - high volume scenario
INSERT INTO job_executions (id, job_id, scheduled_time, started_at, completed_at, status, attempt_number, claimed_by_node, error_message, created_at, updated_at) VALUES
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440011'::uuid, '750e8400-e29b-41d4-a716-446655440005'::uuid, now() - interval '15 seconds', now() - interval '14 seconds', now() - interval '10 seconds', 'SUCCESS', 1, 'node-0', NULL, now() - interval '15 seconds', now() - interval '10 seconds'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440012'::uuid, '750e8400-e29b-41d4-a716-446655440005'::uuid, now() - interval '1 minute 15 seconds', now() - interval '1 minute 14 seconds', now() - interval '1 minute 10 seconds', 'SUCCESS', 1, 'node-1', NULL, now() - interval '1 minute 15 seconds', now() - interval '1 minute 10 seconds'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440013'::uuid, '750e8400-e29b-41d4-a716-446655440006'::uuid, now() - interval '3 hours', now() - interval '3 hours' - interval '5 minutes', now() - interval '3 hours' - interval '2 minutes', 'SUCCESS', 1, 'node-2', NULL, now() - interval '3 hours', now() - interval '3 hours' - interval '2 minutes');

-- Startup executions - unstable service scenario
INSERT INTO job_executions (id, job_id, scheduled_time, started_at, completed_at, status, attempt_number, claimed_by_node, error_message, created_at, updated_at) VALUES
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440014'::uuid, '750e8400-e29b-41d4-a716-446655440009'::uuid, now() - interval '30 minutes', now() - interval '29 minutes', NULL, 'FAILED', 1, 'node-0', 'Error 503: Service Unavailable', now() - interval '30 minutes', now() - interval '30 minutes'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440015'::uuid, '750e8400-e29b-41d4-a716-446655440009'::uuid, now() - interval '30 minutes', now() - interval '28 minutes', NULL, 'FAILED', 2, 'node-1', 'Error 503: Service Unavailable', now() - interval '28 minutes', now() - interval '28 minutes'),
                                                                                                                                                                      ('850e8400-e29b-41d4-a716-446655440016'::uuid, '750e8400-e29b-41d4-a716-446655440009'::uuid, now() - interval '30 minutes', now() - interval '27 minutes', now() - interval '26 minutes', 'SUCCESS', 3, 'node-2', NULL, now() - interval '27 minutes', now() - interval '26 minutes');

COMMIT;

-- Verify data was inserted
SELECT
    (SELECT COUNT(*) FROM tenants) as total_tenants,
    (SELECT COUNT(*) FROM api_keys) as total_api_keys,
    (SELECT COUNT(*) FROM jobs) as total_jobs,
    (SELECT COUNT(*) FROM job_executions) as total_executions;
