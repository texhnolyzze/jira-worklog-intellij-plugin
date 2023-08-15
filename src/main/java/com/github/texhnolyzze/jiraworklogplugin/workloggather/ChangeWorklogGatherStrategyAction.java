package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class ChangeWorklogGatherStrategyAction extends AnAction {

    private final WorklogGatherStrategyEnum strategy;

    protected ChangeWorklogGatherStrategyAction(final WorklogGatherStrategyEnum strategy) {
        this.strategy = strategy;
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            state.setWorklogSummaryGatherStrategy(strategy);
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
        if (state.getWorklogSummaryGatherStrategy() == strategy) {
            e.getPresentation().setIcon(AllIcons.Actions.Checked);
        } else {
            e.getPresentation().setIcon(null);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public static class ToRestApiV2 extends ChangeWorklogGatherStrategyAction {
        public ToRestApiV2() {
            super(WorklogGatherStrategyEnum.REST_API_V2);
        }
    }

    public static class ToTimesheetGadget extends ChangeWorklogGatherStrategyAction {
        public ToTimesheetGadget() {
            super(WorklogGatherStrategyEnum.TIMESHEET_GADGET);
        }
    }

    public static class ToTempoTimesheets extends ChangeWorklogGatherStrategyAction {
        public ToTempoTimesheets() {
            super(WorklogGatherStrategyEnum.TEMPO_TIMESHEETS);
        }
    }

}
