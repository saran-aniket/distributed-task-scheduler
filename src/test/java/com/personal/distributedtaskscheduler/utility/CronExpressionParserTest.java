package com.personal.distributedtaskscheduler.utility;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CronExpressionParserTest {

    private final CronExpressionParser cronExpressionParser = new CronExpressionParser();

    @Test
    void nextFireTime_returnsNextMinuteForEveryMinuteExpression() {
        Instant from = Instant.parse("2026-09-05T10:15:30Z");

        Instant nextFireTime = cronExpressionParser.nextFireTime("* * * * *", from);

        assertThat(nextFireTime).isEqualTo(Instant.parse("2026-09-05T10:15:31Z"));
    }

    @Test
    void nextFireTime_returnsNextScheduledDayForSpecificHourExpression() {
        Instant from = Instant.parse("2026-09-05T10:15:30Z");

        Instant nextFireTime = cronExpressionParser.nextFireTime("0 14 * * *", from);

        assertThat(nextFireTime).isEqualTo(Instant.parse("2026-09-05T14:00:00Z"));
    }

    @Test
    void nextFireTime_returnsNextMatchingWeekdayForDayOfWeekExpression() {
        Instant from = Instant.parse("2026-09-05T10:15:30Z");

        Instant nextFireTime = cronExpressionParser.nextFireTime("30 9 * * 1,3,5", from);

        assertThat(nextFireTime).isEqualTo(Instant.parse("2026-09-07T09:30:00Z"));
    }

    @Test
    void nextFireTime_throwsUsefulExceptionForInvalidCronExpression() {
        assertThatThrownBy(() -> cronExpressionParser.nextFireTime("invalid cron", Instant.parse("2026-09-05T10:15:30Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression: invalid cron")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
