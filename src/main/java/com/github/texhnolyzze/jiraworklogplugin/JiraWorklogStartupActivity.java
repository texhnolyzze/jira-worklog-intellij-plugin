package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class JiraWorklogStartupActivity extends ShelveChangesManager.PostStartupActivity {

    private static final Logger logger = Logger.getInstance(JiraWorklogStartupActivity.class);

    private ScheduledFuture<?> schedule;

    public static JiraWorklogStartupActivity getInstance(final Project project) {
        return project.getService(JiraWorklogStartupActivity.class);
    }

    @Override
    public void runActivity(@NotNull final Project project) {
        logger.info("Executing Jira Worklog Plugin startup actions...");
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project).getState();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            state.setClosed(false);
            final String currentBranch = Util.getCurrentBranch(project);
            if (currentBranch != null) {
                logger.info(
                    String.format(
                        "Found current branch %s",
                        currentBranch
                    )
                );
                init(currentBranch, state, project);
            } else {
                logger.info("Current branch not found. Scheduling periodic task, which will try to initialize it");
                this.schedule = AppExecutorUtil.getAppScheduledExecutorService().schedule(
                    () -> initFromCurrentBranch(project),
                    5L,
                    TimeUnit.SECONDS
                );
            }
        }
        TimerUpdater.getInstance(project).setup(project);
    }

    private void initFromCurrentBranch(final @NotNull Project project) {
        final String branch;
        final JiraWorklogPluginState state;
        try {
            branch = Util.getCurrentBranch(project);
            state = JiraWorklogPluginState.getInstance(project);
        } catch (final AlreadyDisposedException ex) {
            cancel();
            return;
        }
        if (branch != null) {
            logger.info(String.format("Found current branch %s inside periodic task", branch));
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (state) {
                init(branch, state, project);
            }
        } else {
            logger.info("Git repository still not initialized");
            AppExecutorUtil.getAppScheduledExecutorService().schedule(
                () -> initFromCurrentBranch(project),
                5L,
                TimeUnit.SECONDS
            );
        }
    }

    void cancel() {
        if (schedule != null) {
            schedule.cancel(true);
        }
    }

    private void init(
        final String currentBranch,
        final JiraWorklogPluginState state,
        final @NotNull Project project
    ) {
        state.getActiveTimers().clear();
        final Timer activeTimer = state.getTimer(currentBranch, project);
        activeTimer.resume(project);
        for (final Timer timer : state.getTimers().values()) {
            if (timer != activeTimer) {
                timer.pause(project);
            }
        }
        state.setLastBranch(currentBranch);
    }

}
