package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents user's unit of work in particular branch
 */
public class UnitOfWork {

    /**
     * Branch in which user worked
     */
    private String branch;

    /**
     * When user started working
     */
    private final ZonedDateTime started;

    /**
     * How long it worked in this branch
     */
    private final Duration duration;

    public UnitOfWork(
        final String branch,
        final ZonedDateTime started,
        final Duration duration
    ) {
        this.branch = branch;
        this.started = started;
        this.duration = duration;
    }

    public UnitOfWork(final String serialized) {
        final String[] split = serialized.split("\\\\");
        this.branch = split[0];
        this.started = ZonedDateTime.parse(split[1]);
        this.duration = Duration.parse(split[2]);
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(final String branch) {
        this.branch = branch;
    }

    public ZonedDateTime getStarted() {
        return started;
    }

    public Duration getDuration() {
        return duration;
    }

    public Duration findIntersection(final JiraWorklog worklog) {
        return Util.getIntersection(
            started,
            started.plus(duration),
            worklog.getStartTime(),
            worklog.getStartTime().plus(worklog.getTimeSpent())
        );
    }

    @Override
    public String toString() {
        return "UnitOfWork{" +
            "branch='" + branch + '\'' +
            ", started=" + started +
            ", duration=" + duration +
            '}';
    }

    public static class UnitOfWorkListConverter extends Converter<List<UnitOfWork>> {

        @Override
        public @Nullable List<UnitOfWork> fromString(@NotNull final String value) {
            return Arrays.stream(value.split("\n")).map(UnitOfWork::new).collect(Collectors.toList());
        }

        @Override
        public @Nullable String toString(@NotNull final List<UnitOfWork> value) {
            return value.stream().map(
                unit -> unit.getBranch() + "\\" + unit.getStarted() + "\\" + unit.getDuration()
            ).collect(Collectors.joining("\n"));
        }

    }

}
