package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.github.texhnolyzze.jiraworklogplugin.utils.GitUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class PauseAllTimersAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final String branch = GitUtils.getCurrentBranch(project);
        synchronized (state) {
            state.getTimers().values().forEach(timer -> timer.pause(project));
            state.appendUnitOfWork(state.actualUnitOfWorkForBranch(branch, project));
        }
    }

}
