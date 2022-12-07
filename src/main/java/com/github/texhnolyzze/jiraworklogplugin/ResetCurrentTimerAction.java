package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ResetCurrentTimerAction extends AbstractCurrentTimerAction {

    @Override
    protected void doAction(
        final @NotNull Timer timer,
        final @NotNull Project project,
        final @NotNull String branch,
        final @NotNull JiraWorklogPluginState state
    ) {
        timer.reset(project);
        state.getTimeSeries().removeIf(work -> Objects.equals(work.getBranch(), branch));
    }

}
