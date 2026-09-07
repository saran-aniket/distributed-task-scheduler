package com.personal.distributedtaskscheduler.utility;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class CronExpressionParser {
    //All Timezones are UTC
    public Instant nextFireTime(String cronExpression, Instant from) {
        try {
            if(cronExpression != null && from != null) {
                CronExpression cron = CronExpression.parse(normalizeCronExpression(cronExpression));
                ZonedDateTime nextFireTime = cron.next(from.atZone(ZoneOffset.UTC));

                return nextFireTime != null ? nextFireTime.toInstant() : null;
            } else {
                throw new IllegalArgumentException("Invalid cron expression or from date");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }

    public String normalizeCronExpression(String cronExpression) {
        String trimmed = cronExpression == null ? null : cronExpression.trim();
        if (trimmed == null) {
            throw new IllegalArgumentException("Invalid cron expression: null");
        }

        String[] parts = trimmed.split("\\s+");
        if (parts.length == 5) {
            return "* " + trimmed;
        }

        return trimmed;
    }
}
