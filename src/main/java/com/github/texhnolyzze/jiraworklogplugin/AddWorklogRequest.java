package com.github.texhnolyzze.jiraworklogplugin;

class AddWorklogRequest {

    static final String PLUGIN_MARK = "Created by Jira Worklog Plugin\n";

    private final Long timeSpentSeconds;
    private final String comment;
    private final String started;

    AddWorklogRequest(
        final Long timeSpentSeconds,
        final String comment,
        final String started
    ) {
        this.timeSpentSeconds = timeSpentSeconds;
        this.started = started;
        this.comment = comment == null ? PLUGIN_MARK : PLUGIN_MARK + comment;
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

}
