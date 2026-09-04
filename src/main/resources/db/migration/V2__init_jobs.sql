-- Job definitions
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id),
    name VARCHAR(255) NOT NULL,
    job_type VARCHAR(50) NOT NULL,        -- HTTP_CALLBACK, INTERNAL_TASK
    cron_expression VARCHAR(100),          -- nullable for one-off jobs
    payload JSONB,
    webhook_url TEXT,
    max_retries INT DEFAULT 3,
    backoff_seconds INT DEFAULT 30,
    misfire_policy VARCHAR(20) DEFAULT 'SKIP', -- SKIP or FIRE_ONCE
    status VARCHAR(20) DEFAULT 'ACTIVE',   -- ACTIVE, PAUSED, DELETED
    next_fire_time TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_jobs_next_fire ON jobs (next_fire_time) WHERE status = 'ACTIVE';

-- Individual execution attempts (audit trail + retry tracking)
CREATE TABLE job_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES jobs(id),
    scheduled_time TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,   -- PENDING, CLAIMED, RUNNING, SUCCESS, FAILED, TIMED_OUT
    attempt_number INT DEFAULT 1,
    claimed_by_node VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ,
    UNIQUE (job_id, scheduled_time, attempt_number)
);
CREATE INDEX idx_executions_status ON job_executions (status);