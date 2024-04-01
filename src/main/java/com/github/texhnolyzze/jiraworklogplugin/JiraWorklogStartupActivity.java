package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.github.texhnolyzze.jiraworklogplugin.timer.TimerUpdater;
import com.github.texhnolyzze.jiraworklogplugin.utils.GitUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.util.concurrency.AppExecutorUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ExtensionRegisteredAsServiceOrComponent")
public class JiraWorklogStartupActivity implements ProjectActivity {

    private static final Logger logger = Logger.getInstance(JiraWorklogStartupActivity.class);

    private ScheduledFuture<?> schedule;

    public static JiraWorklogStartupActivity getInstance(final Project project) {
        return project.getService(JiraWorklogStartupActivity.class);
    }

    @Nullable
    @Override
    public Object execute(@NotNull final Project project, @NotNull final Continuation<? super Unit> continuation) {
        logger.info("Executing Jira Worklog Plugin startup actions...");
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project).getState();
        synchronized (state) {
            state.setClosed(false);
            final String currentBranch = GitUtils.getCurrentBranch(project);
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
        return null;
    }

    private void initFromCurrentBranch(final @NotNull Project project) {
        final String branch;
        final JiraWorklogPluginState state;
        try {
            branch = GitUtils.getCurrentBranch(project);
            state = JiraWorklogPluginState.getInstance(project);
        } catch (final AlreadyDisposedException ex) {
            cancel();
            return;
        }
        if (branch != null) {
            logger.info(String.format("Found current branch %s inside periodic task", branch));
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
