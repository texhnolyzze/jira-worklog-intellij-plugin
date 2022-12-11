package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.workloggather.HowToDetermineWhenUserStartedWorkingOnIssue;

import java.time.Duration;
import java.time.ZonedDateTime;

public class JiraWorklog {

    private final ZonedDateTime startTime;
    private final Duration timeSpent;
    private final String key;
    private final String comment;
    private final String author;

    public JiraWorklog(
        final ZonedDateTime startTime,
        final Duration timeSpent,
        final String key,
        final String comment,
        final String author,
        final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        this.author = author;
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

    public boolean isIssuedByPlugin() {
        return comment != null && comment.startsWith(AddWorklogRequest.PLUGIN_MARK);
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public String toString() {
        return "JiraWorklog{" +
            "startTime=" + startTime +
            ", timeSpent=" + timeSpent +
            ", key='" + key + '\'' +
            ", comment='" + comment + '\'' +
            ", author='" + author + '\'' +
            '}';
    }

}
