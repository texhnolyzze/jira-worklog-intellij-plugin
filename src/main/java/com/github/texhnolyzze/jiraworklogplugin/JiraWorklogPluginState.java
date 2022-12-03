package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@State(name = "com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState")
public class JiraWorklogPluginState implements PersistentStateComponent<JiraWorklogPluginState> {

    @OptionTag(converter = Timer.TimerMapConverter.class)
    private Map<String, Timer> timers;
    private String lastBranch;
    private String jiraUrl;
    private Map<String, String> commitMessages;
    private boolean showDialogOnExit = true;
    private boolean showDialogOnBranchChange = true;
    private boolean showDialogOnGitPush = true;
    private boolean closed;

    /**
     * Normally should be singleton
     */
    @Transient
    private final Set<Timer> activeTimers = new HashSet<>();

    @NotNull
    public Map<String, Timer> getTimers() {
        if (timers == null) {
            timers = new HashMap<>();
        }
        return timers;
    }

    @NotNull
    public Timer getTimer(@NotNull final String key, final Project project) {
        return getTimers().computeIfAbsent(
            key,
            unused -> new Timer(project)
        );
    }

    public void setTimers(@Nullable final Map<String, Timer> timers) {
        this.timers = timers == null ? new HashMap<>() : timers;
    }

    Set<Timer> getActiveTimers() {
        return activeTimers;
    }

    public String getLastBranch() {
        return lastBranch;
    }

    public void setLastBranch(final String lastBranch) {
        this.lastBranch = lastBranch;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public void setJiraUrl(final String jiraUrl) {
        this.jiraUrl = jiraUrl;
    }

    public @NotNull Map<String, String> getCommitMessages() {
        return commitMessages == null ? commitMessages = new HashMap<>() : commitMessages;
    }

    public void setCommitMessages(@Nullable final Map<String, String> commitMessages) {
        this.commitMessages = commitMessages == null ? new HashMap<>() : commitMessages;
    }

    public boolean isShowDialogOnExit() {
        return showDialogOnExit;
    }

    public void setShowDialogOnExit(final boolean showDialogOnExit) {
        this.showDialogOnExit = showDialogOnExit;
    }

    public boolean isShowDialogOnBranchChange() {
        return showDialogOnBranchChange;
    }

    public void setShowDialogOnBranchChange(final boolean showDialogOnBranchChange) {
        this.showDialogOnBranchChange = showDialogOnBranchChange;
    }

    public boolean isShowDialogOnGitPush() {
        return showDialogOnGitPush;
    }

    public void setShowDialogOnGitPush(final boolean showDialogOnGitPush) {
        this.showDialogOnGitPush = showDialogOnGitPush;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(final boolean closed) {
        this.closed = closed;
    }

    @Override
    public @NotNull JiraWorklogPluginState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull final JiraWorklogPluginState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static JiraWorklogPluginState getInstance(final Project project) {
        return project.getService(JiraWorklogPluginState.class);
    }

}
