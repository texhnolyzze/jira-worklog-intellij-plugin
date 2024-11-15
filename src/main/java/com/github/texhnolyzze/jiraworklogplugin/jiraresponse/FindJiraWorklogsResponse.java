package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FindJiraWorklogsResponse extends JiraResponse {

    private final List<JiraWorklog> worklogs;

    private FindJiraWorklogsResponse(
            final List<JiraWorklog> worklogs,
            final String error
    ) {
        super(error);
        this.worklogs = worklogs;
    }

    public static FindJiraWorklogsResponse error(@NotNull final String error) {
        return new FindJiraWorklogsResponse(null, error);
    }

    public static FindJiraWorklogsResponse success(final List<JiraWorklog> worklogs) {
        return new FindJiraWorklogsResponse(worklogs, null);
    }

    public List<JiraWorklog> getWorklogs() {
        return worklogs;
    }

    @Override
    public String toString() {
        return "FindJiraWorklogsResponse{" +
                "worklogs=" + worklogs +
                "} " + super.toString();
    }

}
