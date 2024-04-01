package com.github.texhnolyzze.jiraworklogplugin.utils;

import org.apache.commons.collections.ComparatorUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Comparator;

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
        final ZonedDateTime beginMax = (ZonedDateTime) ComparatorUtils.max(start1, start2, Comparator.naturalOrder());
        final ZonedDateTime endMin = (ZonedDateTime) ComparatorUtils.min(end1, end2, Comparator.naturalOrder());
        if (beginMax.compareTo(endMin) <= 0) {
            return Duration.between(beginMax, endMin);
        } else {
            return Duration.ZERO;
        }
    }

}
