package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;
import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.TodayWorklogSummaryResponse;

public abstract class WorklogGatherStrategy {

    protected final JiraClient client;

    protected WorklogGatherStrategy(final JiraClient client) {
        this.client = client;
    }

    public abstract TodayWorklogSummaryResponse get(
            final String jiraUrl,
            final String email,
            final String password,
            final HowToDetermineWhenUserStartedWorkingOnIssue how
    );

}
