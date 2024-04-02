package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.enums.WorklogGatherStrategyEnum;
import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private WorklogGatherStrategyEnum worklogSummaryGatherStrategy = WorklogGatherStrategyEnum.REST_API_V2;
    private HowToDetermineWhenUserStartedWorkingOnIssue howToDetermineWhenUserStartedWorkingOnIssue = HowToDetermineWhenUserStartedWorkingOnIssue.SUBTRACT_TIME_SPENT;
    @OptionTag(converter = UnitOfWork.UnitOfWorkListConverter.class)
    private List<UnitOfWork> timeSeries;

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

    public Set<Timer> getActiveTimers() {
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
        if (commitMessages == null) {
            commitMessages = new HashMap<>();
        }
        return commitMessages;
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

    public WorklogGatherStrategyEnum getWorklogSummaryGatherStrategy() {
        return worklogSummaryGatherStrategy;
    }

    public void setWorklogSummaryGatherStrategy(final WorklogGatherStrategyEnum worklogSummaryGatherStrategy) {
        this.worklogSummaryGatherStrategy = worklogSummaryGatherStrategy;
    }

    public HowToDetermineWhenUserStartedWorkingOnIssue getHowToDetermineWhenUserStartedWorkingOnIssue() {
        return howToDetermineWhenUserStartedWorkingOnIssue;
    }

    public void setHowToDetermineWhenUserStartedWorkingOnIssue(final HowToDetermineWhenUserStartedWorkingOnIssue howToDetermineWhenUserStartedWorkingOnIssue) {
        this.howToDetermineWhenUserStartedWorkingOnIssue = howToDetermineWhenUserStartedWorkingOnIssue;
    }

    public @NotNull List<UnitOfWork> getTimeSeries() {
        if (timeSeries == null) {
            timeSeries = new ArrayList<>();
        }
        return timeSeries;
    }

    public void setTimeSeries(@Nullable final List<UnitOfWork> timeSeries) {
        this.timeSeries = timeSeries == null ? new ArrayList<>() : timeSeries;
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

    /**
     * Method calculates time spent in last unit of work for {@code branch}.<br><br>
     * If no previous units of work exists (meaning there was no branch changes, project closes or other events),
     * then last unit of work is just now() - time spent in {@code branch}<br><br>
     * If previous units of work exists, then we need to sum up all their durations and subtract it from time spent in {@code branch}
     * <br><br>
     */
    public UnitOfWork actualUnitOfWorkForBranch(final String branch, final Project project) {
        final List<UnitOfWork> branchUnitsOfWork = getTimeSeries().stream().filter(
            work -> Objects.equals(work.getBranch(), branch)
        ).collect(Collectors.toList());
        final @NotNull Timer timer = getTimer(branch, project);
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        if (branchUnitsOfWork.isEmpty()) {
            final Duration timeSpent = timer.toDuration();
            return new UnitOfWork(
                branch,
                now.minus(timeSpent),
                timeSpent
            );
        } else {
            final Duration totalSpentOnPreviousUnitsOfWork = branchUnitsOfWork.stream().map(
                UnitOfWork::getDuration
            ).reduce(Duration.ZERO, Duration::plus);
            final Duration timeSpentOnActualUnitOfWork = timer.toDuration().minus(totalSpentOnPreviousUnitsOfWork);
            return new UnitOfWork(
                branch,
                now.minus(timeSpentOnActualUnitOfWork),
                timeSpentOnActualUnitOfWork
            );
        }
    }

    public void appendUnitOfWork(final UnitOfWork unitOfWork) {
        getTimeSeries().add(unitOfWork);
    }

}
