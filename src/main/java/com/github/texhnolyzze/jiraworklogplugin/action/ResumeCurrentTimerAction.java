package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ResumeCurrentTimerAction extends AbstractCurrentTimerAction {

    @Override
    protected void doAction(
        final @NotNull Timer timer,
        final @NotNull Project project,
        final @NotNull String branch,
        final @NotNull JiraWorklogPluginState state
    ) {
        timer.resume(project);
    }

}
