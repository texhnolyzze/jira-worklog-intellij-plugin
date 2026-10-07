package com.github.texhnolyzze.jiraworklogplugin.utils;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class IntersectionUtilsTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static ZonedDateTime at(final int hour, final int minute) {
        return ZonedDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(hour, minute), ZONE);
    }

    @Test
    public void givenPartiallyOverlappingIntervals_whenGetIntersection_shouldReturnOverlapDuration() {
        assertThat(
            IntersectionUtils.getIntersection(
                at(10, 0), at(11, 0),
                at(10, 30), at(11, 30)
            )
        ).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    public void givenReversedIntervalOrder_whenGetIntersection_shouldReturnSameOverlapDuration() {
        assertThat(
            IntersectionUtils.getIntersection(
                at(10, 30), at(11, 30),
                at(10, 0), at(11, 0)
            )
        ).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    public void givenFullyContainedInterval_whenGetIntersection_shouldReturnContainedDuration() {
        assertThat(
            IntersectionUtils.getIntersection(
                at(10, 0), at(12, 0),
                at(10, 30), at(11, 0)
            )
        ).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    public void givenTouchingIntervals_whenGetIntersection_shouldReturnZero() {
        assertThat(
            IntersectionUtils.getIntersection(
                at(10, 0), at(11, 0),
                at(11, 0), at(12, 0)
            )
        ).isEqualTo(Duration.ZERO);
    }

    @Test
    public void givenDisjointIntervals_whenGetIntersection_shouldReturnZero() {
        assertThat(
            IntersectionUtils.getIntersection(
                at(10, 0), at(11, 0),
                at(12, 0), at(13, 0)
            )
        ).isEqualTo(Duration.ZERO);
    }

}
