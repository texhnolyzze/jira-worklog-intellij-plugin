package com.github.texhnolyzze.jiraworklogplugin;

class AddWorklogRequest {

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
        this.comment = comment;
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

}
