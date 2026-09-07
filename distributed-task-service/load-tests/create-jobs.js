import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    create_jobs: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 10,
      maxVUs: 50,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)', 'p(90)'],
};

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomCron() {
  const minute = randomInt(0, 59);
  const hour = randomInt(0, 23);
  return `${minute} ${hour} * * *`;
}

function randomJobType() {
  return Math.random() < 0.5 ? 'HTTP_CALLBACK' : 'INTERNAL_JOB';
}

function randomPayload(jobType) {
  if (jobType === 'HTTP_CALLBACK') {
    return {
      url: 'https://example.com/webhook',
      attempt: randomInt(1, 5),
      source: 'k6',
    };
  }

  return {
    baseDurationMs: randomInt(20, 120),
    jitterMs: randomInt(0, 80),
    shouldFail: Math.random() < 0.2,
    failureReason: 'simulated failure',
  };
}

export default function () {
  const jobType = randomJobType();
  const payload = randomPayload(jobType);
  const body = JSON.stringify({
    name: `load-job-${__VU}-${__ITER}-${Date.now()}`,
    cronExpression: randomCron(),
    jobType,
    payLoad: JSON.stringify(payload),
    webhookUrl: jobType === 'HTTP_CALLBACK' ? `${BASE_URL}/webhook` : null,
  });

  const res = http.post(`${BASE_URL}/api/v1/jobs`, body, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
