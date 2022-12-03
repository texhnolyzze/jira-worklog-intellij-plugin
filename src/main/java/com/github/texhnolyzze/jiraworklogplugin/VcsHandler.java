package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.dvcs.push.*;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.BranchChangeListener;
import com.intellij.vcs.log.VcsFullCommitDetails;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.texhnolyzze.jiraworklogplugin.Util.stripRemoteName;
import static com.intellij.openapi.util.text.StringUtil.stripHtml;
import static com.intellij.openapi.util.text.StringUtil.stripQuotesAroundValue;
import static git4idea.branch.GitBranchUtil.stripRefsPrefix;


public class VcsHandler implements Notifications, PrePushHandler, BranchChangeListener {

    private static final Logger logger = Logger.getInstance(VcsHandler.class);

    private static final Pattern PUSH_PATTERN = Pattern.compile(
        "(Pushed \\d+ commits? to (?<branch0>.+))|" +
        "(Pushed \\d+ commits? to (?<branch1>.+), and tag .+ to .+)|" +
        "(Pushed \\d+ commits? to (?<branch2>.+), and \\d+ tags to .+)|" +
        "(Pushed tag .+ to (?<branch3>.+))|" +
        "(Pushed \\d tags to (?<branch4>.+))|" +
        "(Pushed .+ to new branch (?<branch5>.+))|" +
        "(Pushed .+ to new branch (?<branch6>.+), and tag .+ to .+)|" +
        "(Pushed .+ to new branch (?<branch7>.+), and \\d tags to .+)|" +
        "(Force pushed .+ to (?<branch8>.+))"
    );

    private static final Pattern MERGE_COMMIT_PATTERN = Pattern.compile(
        "(Merge branch .+ into (?<branch0>.+))|" +
        "(Merge remote-tracking branch .+ into (?<branch1>.+))|" +
        "(Merge branch .+ of .+ into (?<branch2>.+))|" +
        "(Merge remote-tracking branch (?<branchnotarget>.+))"
    );

    private static final Pattern RENAME_PATTERN = Pattern.compile(
        "Branch (?<srcBranch>.+) was renamed to (?<targetBranch>.+)"
    );

    private final Project project;

    public VcsHandler(final Project project) {
        this.project = project;
    }

    @Override
    public void notify(@NotNull final Notification notification) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final String content = stripHtml(notification.getContent(), "\n");
        if (state.isShowDialogOnGitPush()) {
            final Matcher pushMatcher = PUSH_PATTERN.matcher(content);
            if (pushMatcher.find()) {
                handlePush(state, pushMatcher);
                return;
            }
        }
        final Matcher renameMatcher = RENAME_PATTERN.matcher(content);
        if (renameMatcher.find()) {
            handleRename(state, renameMatcher);
        }
    }

    @SuppressWarnings("java:S2445")
    private void handlePush(final JiraWorklogPluginState state, final Matcher pushMatcher) {
        for (int i = 0; i < 9; i++) {
            final String branch = pushMatcher.group("branch" + i);
            if (branch != null) {
                final String strippedBranch = stripRemoteName(stripRefsPrefix(stripQuotesAroundValue(branch)), project);
                logger.info(String.format("notify: Branch name: %s", strippedBranch));
                final String message;
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (state) {
                    message = replaceIfMergeCommit(
                        state.getCommitMessages().get(strippedBranch),
                        strippedBranch
                    );
                    if (message != null) {
                        state.getCommitMessages().remove(strippedBranch);
                    }
                }
                logger.info(String.format("notify: Commit message (possibly replaced by branch name contained in it): %s", message));
                Util.showWorklogDialog(project, Objects.requireNonNullElse(message, strippedBranch));
                break;
            }
        }
    }

    @SuppressWarnings("java:S2445")
    private void handleRename(final JiraWorklogPluginState state, final Matcher renameMatcher) {
        final String srcBranch = renameMatcher.group("srcBranch");
        final String targetBranch = renameMatcher.group("targetBranch");
        if (srcBranch != null && targetBranch != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (state) {
                final String currentBranch = Util.getCurrentBranch(project);
                state.setLastBranch(currentBranch);
                final Timer srcTimer = state.getTimer(srcBranch, project);
                final Timer targetTimer = state.getTimer(targetBranch, project);
                targetTimer.transfer(srcTimer);
                state.getActiveTimers().remove(srcTimer);
                state.getTimers().remove(srcBranch);
                if (Objects.equals(currentBranch, targetBranch)) {
                    targetTimer.resume(project);
                } else {
                    targetTimer.pause(project);
                }
            }
        }
    }

    @Nullable
    private static String replaceIfMergeCommit(final String message, final String branch) {
        if (message != null) {
            final Matcher commitMatcher = MERGE_COMMIT_PATTERN.matcher(message);
            if (commitMatcher.find()) {
                for (int i = 0; i < 3; i++) {
                    final String targetBranch = commitMatcher.group("branch" + i);
                    if (targetBranch != null) {
                        return targetBranch;
                    }
                }
                final String noTarget = commitMatcher.group("branchnotarget");
                if (noTarget != null) {
                    return branch;
                }
            }
        }
        return message;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        return "Jira Worklog Plugin";
    }

    @Override
    public @NotNull Result handle(@NotNull final List<PushInfo> pushDetails, @NotNull final ProgressIndicator indicator) {
        if (!pushDetails.isEmpty()) {
            final PushInfo info = pushDetails.get(0);
            final PushSpec<PushSource, PushTarget> spec = info.getPushSpec();
            final String localBranchName = spec.getSource().toString();
            final String remoteBranchName = spec.getTarget().toString();
            final String branch = stripRemoteName(stripRefsPrefix(remoteBranchName), project);
            logger.info(String.format("handle: Branch name: %s", localBranchName));
            final List<VcsFullCommitDetails> commits = info.getCommits();
            if (!commits.isEmpty()) {
                final ListIterator<VcsFullCommitDetails> iterator = commits.listIterator(commits.size());
                String commitMessage = null;
                while (iterator.hasPrevious()) {
                    final VcsFullCommitDetails commit = iterator.previous();
                    final String message = commit.getFullMessage();
                    final Matcher matcher = MERGE_COMMIT_PATTERN.matcher(message);
                    if (matcher.find()) {
                        for (int i = 0; i < 3; i++) {
                            final String targetBranch = matcher.group("branch" + i);
                            if (targetBranch != null) {
                                commitMessage = message;
                                break;
                            }
                        }
                        if (commitMessage == null) {
                            final String noTargetBranch = matcher.group("branchnotarget");
                            if (noTargetBranch != null) {
                                commitMessage = message;
                            }
                        }
                        if (commitMessage != null) {
                            break;
                        }
                    } else {
                        final String jiraKey = Util.findJiraKey(message);
                        if (jiraKey != null) {
                            commitMessage = message;
                            logger.info(String.format("handle: Commit message: %s", commitMessage));
                            break;
                        }
                    }
                }
                if (commitMessage != null) {
                    final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (state) {
                        state.getCommitMessages().put(branch, commitMessage);
                    }
                }
            }
        } else {
            logger.info("handle: Push details empty");
        }
        return Result.OK;
    }

    @Override
    public void branchWillChange(@NotNull final String branchName) {
//      Ничего не делаем
    }

    @Override
    public void branchHasChanged(@NotNull final String unused) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final String lastBranch;
        final boolean showDialogOnBranchChange;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            showDialogOnBranchChange = state.isShowDialogOnBranchChange();
            lastBranch = state.getLastBranch();
            if (lastBranch != null) {
                final Timer lastBranchTimer = state.getTimer(lastBranch, project);
                lastBranchTimer.pause(project);
            }
        }
        if (lastBranch != null && showDialogOnBranchChange) {
            Util.showWorklogDialog(project, lastBranch, lastBranch, true);
        }
        final String currentBranch = Util.getCurrentBranch(project);
        if (currentBranch != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (state) {
                final Timer currentBranchTimer = state.getTimer(currentBranch, project);
                currentBranchTimer.resume(project);
                state.setLastBranch(currentBranch);
            }
        }
    }

}
