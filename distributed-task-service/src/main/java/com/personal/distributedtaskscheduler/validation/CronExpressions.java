package com.personal.distributedtaskscheduler.validation;

import java.util.regex.Pattern;

/**
 * Regex for validating standard 5-field cron expressions
 * (minute hour day-of-month month day-of-week).
 *
 * <p>Each field accepts {@code *}, a number, a range {@code n-m}, any of those with a
 * {@code /step}, or a comma-separated list of those tokens. Field <em>ranges</em> (e.g. minute
 * 0-59) are intentionally NOT bounds-checked here — this pattern enforces structure
 * (field count and legal characters), which is what the controller-boundary validation needs.
 */
public final class CronExpressions {

    private CronExpressions() {
    }

    // A single token within a field: '*', a number, or a range 'n-m' — each optionally with a '/step'.
    private static final String TOKEN = "(?:\\*|\\d+(?:-\\d+)?)(?:/\\d+)?";

    // A whole field: one token, or a comma-separated list of tokens.
    private static final String FIELD = TOKEN + "(?:," + TOKEN + ")*";

    /** Matches exactly five whitespace-separated cron fields. */
    public static final String CRON_REGEX = "^" + FIELD + "(?:\\s+" + FIELD + "){4}$";

    public static final Pattern CRON_PATTERN = Pattern.compile(CRON_REGEX);
}
