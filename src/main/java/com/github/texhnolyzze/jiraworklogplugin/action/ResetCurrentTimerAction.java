package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.github.texhnolyzze.jiraworklogplugin.timer.TimerActionUtils;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

class ResetCurrentTimerAction extends AbstractCurrentTimerAction {

    @Override
    protected void doAction(
        final @NotNull Timer timer,
        final @NotNull Project project,
        final @NotNull String branch,
        final @NotNull JiraWorklogPluginState state
    ) {
        TimerActionUtils.resetTimer(branch, project, timer, state);
    }

}
