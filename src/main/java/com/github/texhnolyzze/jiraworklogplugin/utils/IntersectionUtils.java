package com.github.texhnolyzze.jiraworklogplugin.utils;

import java.time.Duration;
import java.time.ZonedDateTime;

public final class IntersectionUtils {

    private IntersectionUtils() {
        throw new UnsupportedOperationException();
    }

    public static Duration getIntersection(
        final ZonedDateTime start1,
        final ZonedDateTime end1,
        final ZonedDateTime start2,
        final ZonedDateTime end2
    ) {
        final ZonedDateTime beginMax = start1.compareTo(start2) >= 0 ? start1 : start2;
        final ZonedDateTime endMin = end1.compareTo(end2) <= 0 ? end1 : end2;
        if (beginMax.compareTo(endMin) <= 0) {
            return Duration.between(beginMax, endMin);
        } else {
            return Duration.ZERO;
        }
    }

}
