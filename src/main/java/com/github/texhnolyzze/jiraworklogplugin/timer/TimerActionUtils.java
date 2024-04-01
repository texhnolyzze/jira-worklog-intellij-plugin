package com.github.texhnolyzze.jiraworklogplugin.timer;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.intellij.openapi.project.Project;

import java.util.Objects;

public final class TimerActionUtils {

    private TimerActionUtils() {
        throw new UnsupportedOperationException();
    }

    public static void resetTimer(final String branch, final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            final Timer timer = state.getTimer(branch, project);
            resetTimer(branch, project, timer, state);
        }
    }

    public static void resetTimer(
        final String branch,
        final Project project,
        final Timer timer,
        final JiraWorklogPluginState state
    ) {
        timer.reset(project);
        state.getTimeSeries().removeIf(work -> Objects.equals(work.getBranch(), branch));
    }

}
