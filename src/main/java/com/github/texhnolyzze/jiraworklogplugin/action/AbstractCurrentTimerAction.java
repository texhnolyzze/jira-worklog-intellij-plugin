package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.github.texhnolyzze.jiraworklogplugin.utils.GitUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCurrentTimerAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final String branch = GitUtils.getCurrentBranch(project);
        if (branch == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            final Timer timer = state.getTimer(branch, project);
            doAction(timer, project, branch, state);
        }
    }

    protected abstract void doAction(
        @NotNull final Timer timer,
        @NotNull final Project project,
        @NotNull final String branch,
        @NotNull final JiraWorklogPluginState state
    );

}
