# Progress Log — Distributed Task Scheduler

Track daily progress against the 7-day plan. Fill in each section at the end of your work session — future-you (and any interviewer reading this repo) will thank present-you.

**Project start date:** YYYY-MM-DD
**Time budget:** 2–3 hrs/day

---

## Day 1 — Setup, HLD Lock-in, Environment

### Tasks completed
- [x] HLD reviewed and finalized
- [x] Spring Boot project scaffolded
- [x] Docker Compose (Postgres + Redis) running
- [x] `application.yml` configured
- [x] Git repo initialized, pushed
- [x] `/actuator/health` green
- [x] k6 installed and verified

### Tests written
| Type | Test name                              | Status |
|---|----------------------------------------|---|
| Unit | NA                                     | ☐ Pass |
| Integration | `AbstractIntegrationTest` context loads | ☐ Pass |
| Load | k6 hello-world verified                | ☐ Pass |

### Notes / Learnings
- Docker compose yml
  - Services, versions, volumes, networks
  - Mounts and binds for data persistence
  - TestContainers (temporary containers) for DBs and Redis integration testing
  - Docker Compose for local development and Run Config path for CI/CD

### Blockers
-

---

## Day 2 — Domain Model & Schema

### Tasks completed
- [x] Flyway migrations (tenants, api_keys, jobs, job_executions, audit_log)
- [x] JPA entities created
- [x] Repositories created
- [x] `POST /jobs`, `GET /jobs`, `GET /jobs/{id}` working

### Tests written
| Type | Test name | Status |
|---|---|---|
| Unit | `JobServiceTest` | ☑ Pass ☐ Fail |
| Unit | `CronExpressionPatternTest` | ☑ Pass ☐ Fail |
| Integration | `JobRepositoryIT` | ☑ Pass ☐ Fail |
| Integration | `JobControllerIT` | ☑ Pass ☐ Fail |

### Notes / Learnings
- Flyway migrations and usage.
- JPA entities and repositories.
- Container Testing
- Mapping JSONs to entities and vice versa.
- MockMVC round trip testing with WebApplicationContext pattern

### Blockers
-

---

## Day 3 — Scheduling Core (Single-Node)

### Tasks completed
- [ ] `CronExpressionParser` implemented
- [ ] `JobScannerService` implemented (with `SKIP LOCKED`)
- [ ] `JobDispatcherService` implemented
- [ ] `HttpCallbackExecutor` implemented
- [ ] Jittered poll interval added

### Tests written
| Type | Test name | Status |
|---|---|---|
| Unit | Cron edge cases | ☐ Pass ☐ Fail |
| Unit | Scanner due-job filtering | ☐ Pass ☐ Fail |
| Unit | WireMock executor tests | ☐ Pass ☐ Fail |
| Integration | End-to-end fire test | ☐ Pass ☐ Fail |
| Integration | `SKIP LOCKED` concurrency | ☐ Pass ☐ Fail |
| Load | Baseline API throughput | Result: _______ req/s, p95: _______ ms |

### Notes / Learnings
-

### Blockers
-

---

## Day 4 — Distributed Coordination (Multi-Node)

### Tasks completed
- [ ] Redisson wired up
- [ ] `DistributedLockService` implemented
- [ ] Heartbeat/lease renewal implemented
- [ ] `RetryPolicy` implemented
- [ ] `MisfireHandler` implemented
- [ ] Multi-node docker-compose setup
- [ ] (Stretch) Leader election

### Tests written
| Type | Test name | Status |
|---|---|---|
| Unit | Lock contention | ☐ Pass ☐ Fail |
| Unit | Backoff math | ☐ Pass ☐ Fail |
| Integration | **Two-node no-double-execution** | ☐ Pass ☐ Fail |
| Integration | Crash + failover | ☐ Pass ☐ Fail |
| Load | Burst due-jobs test | Duplicates found: _______ (target: 0) |

### Notes / Learnings
-

### Blockers
-

---

## Day 5 — Security

### Tasks completed
- [ ] API key generation + hashing
- [ ] `ApiKeyAuthFilter` implemented
- [ ] JWT login for admin
- [ ] RBAC (`@PreAuthorize`) configured
- [ ] Tenant isolation enforced
- [ ] SSRF guard on webhook URLs
- [ ] Rate limiting (Bucket4j)
- [ ] Audit logging wired in
- [ ] Sensitive field encryption

### Tests written
| Type | Test name | Status |
|---|---|---|
| Unit | Auth filter scenarios | ☐ Pass ☐ Fail |
| Unit | SSRF validator | ☐ Pass ☐ Fail |
| Unit | Rate limiter | ☐ Pass ☐ Fail |
| Integration | RBAC / cross-tenant leakage | ☐ Pass ☐ Fail |
| Integration | Audit trail | ☐ Pass ☐ Fail |
| Load | Rate limit under burst (429s not 500s) | ☐ Pass ☐ Fail |

### Notes / Learnings
-

### Blockers
-

---

## Day 6 — Observability

### Tasks completed
- [ ] Micrometer + `/actuator/prometheus`
- [ ] Custom metrics (counters, timer, gauge)
- [ ] Lock contention counter
- [ ] Prometheus + Grafana + Zipkin + Loki in compose
- [ ] JSON structured logging with MDC
- [ ] Tracing wired end-to-end
- [ ] 3 Grafana dashboards built
- [ ] 2+ alert rules configured

### Tests written
| Type | Test name | Status |
|---|---|---|
| Unit | Metrics increment correctly | ☐ Pass ☐ Fail |
| Integration | Prometheus scrape contains custom metrics | ☐ Pass ☐ Fail |
| Integration | Trace ID propagation | ☐ Pass ☐ Fail |
| Load | **Sustained load + live dashboard watch** | p95: _____ ms, p99: _____ ms, error rate: _____% |

### SLO Baseline (record this — you'll compare Day 7 against it)
- p95 execution start latency: _______
- Success rate: _______ %
- Backlog drain time under burst: _______

### Notes / Learnings
-

### Blockers
-

---

## Day 7 — CI/CD, Deployment, Final Load Test, Polish

### Tasks completed
- [ ] GitHub Actions CI (build + test)
- [ ] Docker image build + push to GHCR
- [ ] Deployed to free-tier target: _______________
- [ ] Live smoke test passed
- [ ] README finished (diagrams, setup, API docs, screenshots)
- [ ] Chaos-test demo recorded
- [ ] Final regression pass

### Tests written
| Type | Test name | Status |
|---|---|---|
| Unit | Final cleanup pass | ☐ Pass ☐ Fail |
| Integration | Full suite in CI | ☐ Pass ☐ Fail |
| Load | **Final load test vs deployed instance** | p95: _____ ms, p99: _____ ms, error rate: _____% |

### Comparison vs Day 6 baseline
- Latency: ☐ Improved ☐ Same ☐ Regressed — details: _______
- Error rate: ☐ Improved ☐ Same ☐ Regressed — details: _______

### Notes / Learnings
-

### Blockers
-

---

## Final Retrospective

**What I'm most proud of:**
-

**What I'd do differently if starting over:**
-

**Hardest bug/problem and how I solved it:**
-

**What I'd build next if I had another week:**
-

**One-line summary for a resume/portfolio:**
-
