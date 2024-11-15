package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

import org.jetbrains.annotations.NotNull;

import java.util.NavigableSet;

public class FindJiraIssuesResponse extends JiraResponse {

    private final NavigableSet<JiraIssue> issues;

    private FindJiraIssuesResponse(
            final NavigableSet<JiraIssue> issues,
            final String error
    ) {
        super(error);
        this.issues = issues;
    }

    public NavigableSet<JiraIssue> getIssues() {
        return issues;
    }

    public static FindJiraIssuesResponse success(final NavigableSet<JiraIssue> issues) {
        return new FindJiraIssuesResponse(issues, null);
    }

    public static FindJiraIssuesResponse error(@NotNull final String error) {
        return new FindJiraIssuesResponse(null, error);
    }

    @Override
    public String toString() {
        return "FindJiraIssuesResponse{" +
                "issues=" + issues +
                "} " + super.toString();
    }

}
