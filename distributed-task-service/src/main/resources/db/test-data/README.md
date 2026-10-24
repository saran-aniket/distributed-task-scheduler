# Test Data Configuration

This directory contains test data initialization scripts that run exclusively in test environments.

## Overview

The test data script (`V999_init-test-data.sql`) uses **Flyway migration versioning** and is configured to run only when the `test` profile is active. This ensures test data never loads in production or development environments.

## Files

- **V999_init-test-data.sql** - Main test data script with scenario-based data sets
- **README.md** - This file

## Test Data Structure

### 1. Tenants (3 scenarios)
- **Demo SaaS Company 1** - Typical SaaS customer with report generation workflows
- **Enterprise Cloud Platform** - High-volume, enterprise customer with compliance needs
- **Startup Rapid-Growth-Co** - Recently onboarded startup with experimental features

### 2. API Keys (8 keys with role-based access)
- **Admin Keys** - Full access scenario
- **Owner Keys** - Ownership-level permissions
- **Viewer Keys** - Read-only access
- **Revoked Keys** - Security scenario with disabled credentials

### 3. Jobs (9 different job types)
- **Hourly Report Generation** - Recurring HTTP callback
- **Daily Data Sync** - Internal task with FIRE_ONCE policy
- **Weekly Backup** - Long-running scheduled job
- **Paused Notification Task** - Testing job pause/resume
- **Real-time Metric Collection** - High-frequency execution (every minute)
- **Batch User Provisioning** - Large batch processing
- **Compliance Audit Export** - Compliance reporting workflow
- **Cache Warming** - Experimental internal task
- **Unstable External API** - High-retry scenario for flaky services

### 4. Job Executions (16 execution records)
- **Successful executions** - Complete workflow testing
- **Failed with retries** - Retry mechanism testing (attempt 1, 2, 3)
- **Pending executions** - Unscheduled future tasks
- **Claimed executions** - Assigned to node but not yet running
- **Timeout scenarios** - Execution timeout handling
- **Running executions** - Currently in-progress tasks

## Configuration

### Activating Test Profile

Set the Spring profile to `test`:

```bash
# Via environment variable
export SPRING_PROFILES_ACTIVE=test

# Via application properties
spring.profiles.active=test

# Via Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
```

### Customizing Test Data Volume

Edit `application-test.yaml` to adjust placeholder values:

```yaml
spring:
  flyway:
    placeholders:
      test.enabled: 'true'
      test.tenant.count: '3'          # Number of tenants to create
      test.jobs.per.tenant: '5'       # Jobs per tenant
      test.executions.per.job: '10'   # Executions per job
      test.user.prefix: "test-user-"  # Prefix for test email addresses
```

## Data Validation

The script includes a validation query at the end:

```sql
SELECT 
  (SELECT COUNT(*) FROM tenants) as total_tenants,
  (SELECT COUNT(*) FROM api_keys) as total_api_keys,
  (SELECT COUNT(*) FROM jobs) as total_jobs,
  (SELECT COUNT(*) FROM job_executions) as total_executions;
```

Expected output when using defaults:
- `total_tenants`: 3
- `total_api_keys`: 8
- `total_jobs`: 9
- `total_executions`: 16

## Testing Scenarios Covered

✅ **Authentication & Authorization**
- Multiple roles (ADMIN, OWNER, VIEWER)
- Revoked credentials
- Tenant isolation

✅ **Job Lifecycle**
- Active, paused, and deleted statuses
- Different job types (HTTP_CALLBACK, INTERNAL_TASK)
- Various cron expressions and schedules

✅ **Execution States**
- SUCCESS, FAILED, PENDING, CLAIMED, RUNNING, TIMED_OUT
- Retry mechanisms with multiple attempts
- Node-claimed execution tracking

✅ **Error Scenarios**
- Connection timeouts
- Service unavailability (503)
- Execution timeouts
- Misfire policies (SKIP vs FIRE_ONCE)

## Safety Measures

1. **Profile Isolation** - Test data only loads with `test` profile active
2. **Transactional Script** - All data wrapped in `BEGIN/COMMIT` for atomic operations
3. **Flyway Versioning** - Uses versioned migration (V999) for idempotent runs
4. **UUID Consistency** - Fixed UUIDs for reliable test assertions

## Notes

- Test data includes realistic timestamps (relative to `now()`)
- JSON payloads are embedded as test case examples
- Error messages reflect real-world failure scenarios
- All IDs use UUIDs matching production schema
