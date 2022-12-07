package com.github.texhnolyzze.jiraworklogplugin;

import java.time.Duration;
import java.util.List;

public class TodayWorklogSummaryResponse {

    private static final Duration WORKDAY_DURATION = Duration.ofHours(8);

    private final Duration timeSpent;
    private final String error;
    private final List<JiraWorklog> worklogs;

    TodayWorklogSummaryResponse(
        final Duration timeSpent,
        final List<JiraWorklog> worklogs,
        final String error
    ) {
        this.timeSpent = timeSpent;
        this.error = error;
        this.worklogs = worklogs;
    }

    public String getRemainedToLogPretty() {
        return Util.formatAsJiraDuration(WORKDAY_DURATION.minus(timeSpent));
    }

    public String getSpentPretty() {
        return Util.formatAsJiraDuration(timeSpent);
    }

    public String getError() {
        return error;
    }

    public List<JiraWorklog> getWorklogs() {
        return worklogs;
    }

    @Override
    public String toString() {
        return "TodayWorklogSummaryResponse{" +
            "timeSpent=" + timeSpent +
            ", error='" + error + '\'' +
            ", worklogs=" + worklogs +
            '}';
    }

    public static TodayWorklogSummaryResponse success(final Duration timeSpent, final List<JiraWorklog> worklogs) {
        return new TodayWorklogSummaryResponse(timeSpent, worklogs, null);
    }

    public static TodayWorklogSummaryResponse error(final String error) {
        return new TodayWorklogSummaryResponse(null, null, error);
    }

}
