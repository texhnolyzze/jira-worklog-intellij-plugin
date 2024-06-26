package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.timer.TimerUpdater;
import com.github.texhnolyzze.jiraworklogplugin.utils.GitUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.WorklogDialogUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseHandler;
import org.jetbrains.annotations.NotNull;

public class JiraWorklogProjectCloseHandler implements ProjectCloseHandler {

    private static final Logger logger = Logger.getInstance(JiraWorklogProjectCloseHandler.class);

    @Override
    public boolean canClose(@NotNull final Project project) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            if (state.isClosed()) {
                return true;
            }
            state.setClosed(true);
        }
        final String branchName = GitUtils.getCurrentBranch(project);
        logger.info(
            String.format(
                "canClose: Branch name is %s",
                branchName
            )
        );
        synchronized (state) {
            state.getTimers().values().forEach(timer -> timer.pause(project));
            if (branchName != null) {
                state.appendUnitOfWork(state.actualUnitOfWorkForBranch(branchName, project));
            }
        }
        if (branchName != null && state.isShowDialogOnExit()) {
            WorklogDialogUtils.showWorklogDialog(project, branchName, branchName);
            state.getTimer(branchName, project).pause(project);
        }
        final TimerUpdater updater = TimerUpdater.getInstance(project);
        if (updater != null) {
            updater.cancel();
        }
        final JiraWorklogStartupActivity startupActivity = JiraWorklogStartupActivity.getInstance(project);
        if (startupActivity != null) {
            startupActivity.cancel();
        }
        return true;
    }

}
