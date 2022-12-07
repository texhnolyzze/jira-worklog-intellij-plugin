package com.github.texhnolyzze.jiraworklogplugin;

import java.util.List;

public class FindJiraWorklogsResponse {

    private final List<JiraWorklog> worklogs;
    private final String error;

    public FindJiraWorklogsResponse(final List<JiraWorklog> worklogs, final String error) {
        this.worklogs = worklogs;
        this.error = error;
    }

    public List<JiraWorklog> getWorklogs() {
        return worklogs;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "FindJiraWorklogsResponse{" +
            "worklogs=" + worklogs +
            ", error='" + error + '\'' +
            '}';
    }

}
