package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.jirarequest.AddWorklogRequest;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;

public class JiraWorklog {

    private final ZonedDateTime startTime;
    private final Duration timeSpent;
    private final String key;
    private final String comment;
    private final String authorEmailAddress;

    public JiraWorklog(
        final ZonedDateTime startTime,
        final Duration timeSpent,
        final String key,
        final String comment,
        final String authorEmailAddress,
        final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        this.authorEmailAddress = authorEmailAddress;
        this.startTime = how.determine(startTime, timeSpent);
        this.timeSpent = timeSpent;
        this.key = key;
        this.comment = comment;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public Duration getTimeSpent() {
        return timeSpent;
    }

    public String getKey() {
        return key;
    }

    public String getComment() {
        return comment;
    }

    public boolean isIssuedByPlugin(final @NotNull String projectName) {
        return comment != null && comment.startsWith(AddWorklogRequest.getPluginMark(projectName));
    }

    public String getAuthorEmailAddress() {
        return authorEmailAddress;
    }

    @Override
    public String toString() {
        return "JiraWorklog{" +
            "startTime=" + startTime +
            ", timeSpent=" + timeSpent +
            ", key='" + key + '\'' +
            ", comment='" + comment + '\'' +
            ", author='" + authorEmailAddress + '\'' +
            '}';
    }

}
