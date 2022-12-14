package com.github.texhnolyzze.jiraworklogplugin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.util.xmlb.Converter;
import org.apache.commons.lang3.SerializationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @JsonCreator
    public UnitOfWork(
        @JsonProperty("branch") final String branch,
        @JsonProperty("started") final ZonedDateTime started,
        @JsonProperty("duration") final Duration duration
    ) {
        this.branch = branch;
        this.started = started;
        this.duration = duration;
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
            try {
                return Util.OBJECT_MAPPER.readValue(value, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                return new ArrayList<>();
            }
        }

        @Override
        public @Nullable String toString(@NotNull final List<UnitOfWork> value) {
            try {
                return Util.OBJECT_MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new SerializationException(e);
            }
        }

    }

}
