package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;
import com.github.texhnolyzze.jiraworklogplugin.TodayWorklogSummaryResponse;

public abstract class WorklogGatherStrategy {

    protected final JiraClient client;

    protected WorklogGatherStrategy(final JiraClient client) {
        this.client = client;
    }

    public abstract TodayWorklogSummaryResponse get(
        final String jiraUrl,
        final String username,
        final String password,
        final HowToDetermineWhenUserStartedWorkingOnIssue how
    );

}
