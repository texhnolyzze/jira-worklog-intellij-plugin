package com.github.texhnolyzze.jiraworklogplugin;

import java.util.NavigableSet;

class FindJiraIssuesResponse {

    private final NavigableSet<JiraIssue> issues;
    private final String error;

    private FindJiraIssuesResponse(final NavigableSet<JiraIssue> issues, final String error) {
        this.issues = issues;
        this.error = error;
    }

    public NavigableSet<JiraIssue> getIssues() {
        return issues;
    }

    public String getError() {
        return error;
    }

    public static FindJiraIssuesResponse success(final NavigableSet<JiraIssue> issues) {
        return new FindJiraIssuesResponse(issues, null);
    }

    public static FindJiraIssuesResponse error(final String error) {
        return new FindJiraIssuesResponse(null, error);
    }

}
