package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class ChangeHowToDetermineWhenUserStartedWorkingAction extends AnAction {

    private final HowToDetermineWhenUserStartedWorkingOnIssue how;

    protected ChangeHowToDetermineWhenUserStartedWorkingAction(final HowToDetermineWhenUserStartedWorkingOnIssue how) {
        this.how = how;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            state.setHowToDetermineWhenUserStartedWorkingOnIssue(how);
        }
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        if (state.getHowToDetermineWhenUserStartedWorkingOnIssue() == how) {
            e.getPresentation().setIcon(AllIcons.Actions.Checked);
        } else {
            e.getPresentation().setIcon(null);
        }
    }

    public static class LeaveAsIs extends ChangeHowToDetermineWhenUserStartedWorkingAction {
        public LeaveAsIs() {
            super(HowToDetermineWhenUserStartedWorkingOnIssue.LEAVE_AS_IS);
        }
    }

    public static class SubtractFromStarted extends ChangeHowToDetermineWhenUserStartedWorkingAction {
        public SubtractFromStarted() {
            super(HowToDetermineWhenUserStartedWorkingOnIssue.SUBTRACT_TIME_SPENT);
        }
    }

}
