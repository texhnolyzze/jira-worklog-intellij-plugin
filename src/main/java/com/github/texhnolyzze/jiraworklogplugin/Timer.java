package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;

final class Timer {

    private static final Logger logger = Logger.getInstance(Timer.class);

    private long total;
    private long updatedAt;
    private boolean paused;

    Timer(final Project project) {
        reset(project);
    }

    public Timer(final String serialized) {
        final String[] split = serialized.split(";");
        this.total = parseLong(split[0]);
        this.updatedAt = parseLong(split[1]);
        paused = parseBoolean(split[2]);
    }

    void reset(final Project project) {
        total = 0;
        updatedAt = System.nanoTime();
        paused = false;
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            state.getActiveTimers().add(this);
        }
    }

    Duration toDuration() {
        return Duration.ofNanos(total);
    }

    void pause(final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            state.getActiveTimers().remove(this);
        }
        paused = true;
    }

    boolean paused() {
        return paused;
    }

    void resume(final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            state.getActiveTimers().add(this);
        }
        if (!paused) {
            return;
        }
        paused = false;
        updatedAt = System.nanoTime();
    }

    void update(final long updateInterval) {
        if (paused()) {
            return;
        }
        final long now = System.nanoTime();
        final long diff = now - updatedAt;
        if (diff > 0) {
            final long offFactor = diff / updateInterval;
            if (offFactor <= 6) {
                total += diff;
            } else {
                logger.info(String.format("Off factor is %s (greater than 6). Skipping this timer update", offFactor));
            }
        }
        updatedAt = now;
    }

    long getUpdatedAt() {
        return updatedAt;
    }

    public void transfer(final Timer other) {
        this.total += other.total;
    }

    public static class TimerMapConverter extends com.intellij.util.xmlb.Converter<Map<String, Timer>> {

        @Override
        public @Nullable Map<String, Timer> fromString(@NotNull final String value) {
            final String[] values = value.split("\n");
            final Map<String, Timer> result = new HashMap<>(values.length);
            for (final String s : values) {
                final String[] keyValueSplit = s.split("->");
                if (keyValueSplit.length != 2) {
                    continue;
                }
                final String key = keyValueSplit[0];
                final Timer timer = new Timer(keyValueSplit[1]);
                result.put(key, timer);
            }
            return result;
        }

        @Override
        public @Nullable String toString(@NotNull final Map<String, Timer> value) {
            return value.entrySet().stream().map(
                entry -> entry.getKey() + "->" + entry.getValue().serialize()
            ).collect(Collectors.joining("\n"));
        }

    }

    private String serialize() {
        return total + ";" + updatedAt + ";" + paused;
    }

}
