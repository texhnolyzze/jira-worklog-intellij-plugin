package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.utils.GitUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.WorklogDialogUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class ShowWorklogDialogAction extends AnAction {

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final String currentBranch = GitUtils.getCurrentBranch(project);
        if (currentBranch != null) {
            WorklogDialogUtils.showWorklogDialog(project, currentBranch);
        } else {
            JOptionPane.showMessageDialog(
                null,
                "Cannot determine current git branch",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

}
