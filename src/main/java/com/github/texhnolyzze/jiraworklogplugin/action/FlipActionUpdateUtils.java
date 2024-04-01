package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

final class FlipActionUpdateUtils {

    private FlipActionUpdateUtils() {
        throw new UnsupportedOperationException();
    }

    static void update(@NotNull AnActionEvent e, final Predicate<JiraWorklogPluginState> isChecked) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final boolean checked = isChecked.test(state);
        if (checked) {
            e.getPresentation().setIcon(AllIcons.Actions.Checked);
        } else {
            e.getPresentation().setIcon(null);
        }
    }

}
