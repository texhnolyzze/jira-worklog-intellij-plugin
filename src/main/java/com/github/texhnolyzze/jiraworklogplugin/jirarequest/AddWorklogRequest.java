package com.github.texhnolyzze.jiraworklogplugin.jirarequest;

import org.jetbrains.annotations.NotNull;

public class AddWorklogRequest {

    static final String PLUGIN_MARK = "Created by Jira Worklog Plugin: %s\n";

    private final Long timeSpentSeconds;
    private final String comment;
    private final String started;

    public AddWorklogRequest(
        final @NotNull String projectName,
        final Long timeSpentSeconds,
        final String comment,
        final String started
    ) {
        this.timeSpentSeconds = timeSpentSeconds;
        this.started = started;
        final String mark = getPluginMark(projectName);
        this.comment = comment == null ? mark : mark + comment;
    }

    public Long getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public String getComment() {
        return comment;
    }

    public String getStarted() {
        return started;
    }

    @Override
    public String toString() {
        return "AddWorklogRequest{" +
            "timeSpentSeconds=" + timeSpentSeconds +
            ", comment='" + comment + '\'' +
            ", started='" + started + '\'' +
            '}';
    }

    public static String getPluginMark(final @NotNull String projectName) {
        return String.format(PLUGIN_MARK, projectName);
    }

}
