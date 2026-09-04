package com.personal.distributedtaskscheduler.validation;

import com.personal.distributedtaskscheduler.dto.CreateJobRequestDTO;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Table-driven test for the 5-field cron expression regex pattern.
 * Validates that the pattern correctly matches valid cron strings
 * and rejects invalid ones (wrong field count, illegal characters, malformed ranges).
 */
class CronExpressionPatternTest {

    @Test
    void createJobRequestDto_cronExpressionUsesCronPattern() throws Exception {
        Field field = CreateJobRequestDTO.class.getDeclaredField("cronExpression");
        Pattern pattern = field.getAnnotation(Pattern.class);

        assertThat(pattern).isNotNull();
        assertThat(pattern.regexp()).isEqualTo(CronExpressions.CRON_REGEX);
    }

    @ParameterizedTest(name = "{0} should match={1}")
    @CsvSource({
            "'0 0 * * *', true",
            "'0 12 * * *', true",
            "'*/15 * * * *', true",
            "'0 9 1 * *', true",
            "'0 0 * * 0', true",
            "'0 0 1 1 *', true",
            "'30 2 * * 1-5', true",
            "'0 */6 * * *', true",
            "'0,30 * * * *', true",
            "'0 9-17 * * 1-5', true",
            "'0 0 1,15 * *', true",
            "'0 22 * * 5', true",
            "'*/5 * * * *', true",
            "'0 0 31 12 *', true",
            "'59 23 31 12 6', true",
            "'1 2 3 4 5', true",
            "'0 0 * * 0-4', true",
            "'0 0 * * 0,2,4,6', true",
            "'0 0 * *', false",
            "'0 0 * * * *', false",
            "'* * * * * *', false",
            "'0', false",
            "'0 0 @ * *', false",
            "'0 0 * * ?', false",
            "'0 0 * * #', false",
            "'0 0 MON * *', false",
            "'0 JAN * * *', false",
            "'a b c d e', false",
            "'0 0 *- * *', false",
            "'0 0 -5 * *', false",
            "'0 0 1-2-3 * *', false",
            "'0 0 * * *-', false",
            "'0 0 */- * *', false",
            "'0 0 0/ * *', false",
            "'0 0 * /5 *', false",
            "'0 0 * * 1//2', false",
    })
    void testCronExpressionPattern(String cron, boolean shouldMatch) {
        boolean matches = CronExpressions.CRON_PATTERN.matcher(cron).matches();
        assertThat(matches).isEqualTo(shouldMatch)
                .as("Cron expression '%s' should match=%s", cron, shouldMatch);
    }
}
