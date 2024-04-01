package com.github.texhnolyzze.jiraworklogplugin.timer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.github.texhnolyzze.jiraworklogplugin.utils.Utils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.Converter;
import org.apache.commons.lang3.SerializationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Timer {

    private static final Logger logger = Logger.getInstance(Timer.class);

    private long total;
    private boolean paused;

    /**
     * Used to measure elapsed time only
     */
    private long updatedAt;

    /**
     * Used to measure real time when timer was updated
     * since we cannot use {@link System#nanoTime()} for this
     */
    private Instant updatedAtSinceEpoch;

    /**
     * Used only for new timers
     */
    public Timer(final Project project) {
        reset(project);
    }

    @JsonCreator
    public Timer(
        @JsonProperty("total") final long total,
        @JsonProperty("updatedAtSinceEpoch") final Instant updatedAtSinceEpoch,
        @JsonProperty("paused") final boolean paused
    ) {
        this.total = total;
        this.updatedAtSinceEpoch = updatedAtSinceEpoch;
        this.paused = paused;
        updatedAt = System.nanoTime();
    }

    public void reset(final Project project) {
        total = 0;
        updatedAt = System.nanoTime();
        updatedAtSinceEpoch = Instant.now(Clock.systemUTC());
        paused = false;
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            state.getActiveTimers().add(this);
        }
    }

    public Duration toDuration() {
        return Duration.ofNanos(total);
    }

    public void pause(final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            state.getActiveTimers().remove(this);
        }
        paused = true;
    }

    boolean paused() {
        return paused;
    }

    public void resume(final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            state.getActiveTimers().add(this);
        }
        if (!paused) {
            return;
        }
        paused = false;
        updatedAt = System.nanoTime();
        this.updatedAtSinceEpoch = Instant.now(Clock.systemUTC());
    }

    void update(final long updateInterval) {
        if (paused()) {
            return;
        }
        final long now = System.nanoTime();
        final long diff = now - updatedAt;
        if (diff > 0) {
            final long offFactor = diff / updateInterval;
            if (offFactor < 6) {
                total += diff;
            } else {
                logger.info(String.format("Off factor is %s (greater than 6). Skipping this timer update", offFactor));
            }
        }
        updatedAt = now;
        updatedAtSinceEpoch = Instant.now(Clock.systemUTC());
    }

    public long getTotal() {
        return total;
    }

    public boolean isPaused() {
        return paused;
    }

    public Instant getUpdatedAtSinceEpoch() {
        return updatedAtSinceEpoch;
    }

    public void transfer(final Timer other) {
        this.total += other.total;
    }

    @Override
    public String toString() {
        return "Timer{" +
            "total=" + total +
            ", updatedAt=" + updatedAt +
            ", updateAtSinceEpoc=" + updatedAtSinceEpoch +
            ", paused=" + paused +
            '}';
    }

    public static class TimerMapConverter extends Converter<Map<String, Timer>> {

        @Override
        public @Nullable Map<String, Timer> fromString(@NotNull final String value) {
            try {
                return Utils.OBJECT_MAPPER.readValue(value, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                return new HashMap<>(1);
            }
        }

        @Override
        public @Nullable String toString(@NotNull final Map<String, Timer> value) {
            try {
                return Utils.OBJECT_MAPPER.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                throw new SerializationException(e);
            }
        }

    }

}
