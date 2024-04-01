package com.github.texhnolyzze.jiraworklogplugin.utils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JiraDurationUtils {

    static final BigDecimal MINUTES_IN_HOUR = new BigDecimal(60);


    private static final Pattern JIRA_DURATION_PATTERN = Pattern.compile(
            "(?<hours1>(\\d+(([.,])\\d+)?h))\\s+(?<minutes1>(\\d+(([.,])\\d+)?m))|" +
                    "(?<hours2>(\\d+(([.,])\\d+)?h))|" +
                    "(?<minutes2>(\\d+(([.,])\\d+)?m))"
    );

    private JiraDurationUtils() {
        throw new UnsupportedOperationException();
    }

    public static String formatAsJiraDuration(final Duration duration) {
        return duration.toHours() + "h " + duration.toMinutesPart() + "m";
    }

    public static boolean isJiraDuration(String duration) {
        return parseJiraDuration(duration) != null;
    }

    public static Duration parseJiraDuration(String duration) {
        if (duration == null) {
            return null;
        }
        duration = duration.replace(',', '.').strip();
        final Matcher matcher = JIRA_DURATION_PATTERN.matcher(duration);
        final boolean matches = matcher.matches();
        if (matches) {
            final BigDecimal hours;
            final BigDecimal minutes;
            final String hours1 = matcher.group("hours1");
            if (hours1 != null) {
                hours = new BigDecimal(hours1.substring(0, hours1.indexOf('h')));
                final String minutes1 = matcher.group("minutes1");
                minutes = new BigDecimal(minutes1.substring(0, minutes1.indexOf('m')));
            } else {
                final String hours2 = matcher.group("hours2");
                if (hours2 != null) {
                    hours = new BigDecimal(hours2.substring(0, hours2.indexOf('h')));
                    minutes = BigDecimal.ZERO;
                } else {
                    final String minutes2 = matcher.group("minutes2");
                    hours = BigDecimal.ZERO;
                    minutes = new BigDecimal(minutes2.substring(0, minutes2.indexOf('m')));
                }
            }
            final BigDecimal totalMinutes = hours.multiply(MINUTES_IN_HOUR).add(minutes);
            return Duration.ofMinutes(totalMinutes.intValue());
        }
        return null;
    }

}
