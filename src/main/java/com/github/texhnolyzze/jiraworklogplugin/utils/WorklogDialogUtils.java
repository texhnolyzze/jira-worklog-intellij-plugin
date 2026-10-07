package com.github.texhnolyzze.jiraworklogplugin.utils;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogDialog;
import com.intellij.concurrency.ThreadContext;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WorklogDialogUtils {

    private WorklogDialogUtils() {
        throw new UnsupportedOperationException();
    }

    public static void showWorklogDialog(
        final @NotNull Project project,
        final @NotNull String jiraKeyContent
    ) {
        showWorklogDialog(project, jiraKeyContent, GitUtils.getCurrentBranch(project), null);
    }

    public static void showWorklogDialog(
        final @NotNull Project project,
        final @NotNull String jiraKeyContent,
        final @Nullable String branchName
    ) {
        showWorklogDialog(project, jiraKeyContent, branchName, null);
    }

    public static void showWorklogDialog(
        final @NotNull Project project,
        final @NotNull String jiraKeyContent,
        final @Nullable String branchName,
        final @Nullable String transferToBranch
    ) {
        if (StringUtils.isBlank(branchName)) {
            return;
        }
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, branchName, transferToBranch);
        dialog.pack();
        dialog.afterPack();
        dialog.init(JiraKeyUtils.findJiraKey(jiraKeyContent));
        dialog.setLocationRelativeTo(null);
        try (AccessToken ignored = ThreadContext.resetThreadContext()) {
            dialog.setVisible(true);
        }
    }

}
