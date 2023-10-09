package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.project.Project;

import java.util.Objects;

final class TimerActionUtils {

    private TimerActionUtils() {
        throw new UnsupportedOperationException();
    }

    static void resetTimer(final String branch, final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            final Timer timer = state.getTimer(branch, project);
            resetTimer(branch, project, timer, state);
        }
    }

    static void resetTimer(
        final String branch,
        final Project project,
        final Timer timer,
        final JiraWorklogPluginState state
    ) {
        timer.reset(project);
        state.getTimeSeries().removeIf(work -> Objects.equals(work.getBranch(), branch));
    }

}
