package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerUpdater {

    private static final Logger logger = Logger.getInstance(TimerUpdater.class);

    private int numUpdates;
    private ScheduledFuture<?> schedule;

    static TimerUpdater getInstance(final Project project) {
        return project.getService(TimerUpdater.class);
    }

    void setup(final Project project) {
        final long updateInterval = Duration.ofSeconds(10).toNanos();
        this.schedule = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            () -> {
                final JiraWorklogPluginState state;
                try {
                    state = JiraWorklogPluginState.getInstance(project);
                } catch (final AlreadyDisposedException ex) {
                    cancel();
                    return;
                }
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (state) {
                    final Set<Timer> activeTimers = state.getActiveTimers();
                    if (activeTimers.isEmpty()) {
                        logger.warn("No active timers");
                    } else {
                        if (activeTimers.size() > 1) {
                            logger.warn(
                                String.format(
                                    "Number of active timers is greater than 1: %s",
                                    activeTimers.size()
                                )
                            );
                        }
                        for (final Timer timer : activeTimers) {
                            timer.update(updateInterval);
                        }
                    }
                    if (numUpdates % 360 == 0) {
                        int numDeleted = 0;
                        logger.info("Deleting stale timers (paused for more than a week)");
                        for (
                            final Iterator<Map.Entry<String, Timer>> iterator = state.getTimers().entrySet().iterator();
                            iterator.hasNext();
                        ) {
                            final Map.Entry<String, Timer> entry = iterator.next();
                            final Timer timer = entry.getValue();
                            final Duration duration = Duration.between(
                                timer.getUpdatedAtSinceEpoch(),
                                Instant.now(Clock.systemUTC())
                            );
                            if (duration.toDays() > 7) {
                                iterator.remove();
                                state.getCommitMessages().remove(entry.getKey());
                                state.getTimeSeries().removeIf(work -> work.getBranch().equals(entry.getKey()));
                                state.getActiveTimers().remove(timer);
                                numDeleted++;
                            }
                        }
                        if (numDeleted > 0) {
                            logger.info(
                                String.format("Deleted %s stale timers (paused for more than a week)", numDeleted)
                            );
                        }
                        numUpdates = 1;
                    } else {
                        numUpdates++;
                    }
                }
            },
            updateInterval,
            updateInterval,
            TimeUnit.NANOSECONDS
        );
        logger.info("TimerUpdater setup");
    }

    void cancel() {
        if (this.schedule != null) {
            this.schedule.cancel(true);
        }
    }

}
