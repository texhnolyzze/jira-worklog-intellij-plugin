package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklog;
import com.github.texhnolyzze.jiraworklogplugin.utils.JiraDurationUtils;
import org.jetbrains.annotations.NotNull;

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
        return JiraDurationUtils.formatAsJiraDuration(WORKDAY_DURATION.minus(timeSpent));
    }

    public String getSpentPretty() {
        return JiraDurationUtils.formatAsJiraDuration(timeSpent);
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

    public static TodayWorklogSummaryResponse success(@NotNull final List<JiraWorklog> worklogs) {
        return new TodayWorklogSummaryResponse(
            worklogs.stream().map(JiraWorklog::getTimeSpent).reduce(Duration.ZERO, Duration::plus),
            worklogs,
            null
        );
    }

    public static TodayWorklogSummaryResponse error(final String error) {
        return new TodayWorklogSummaryResponse(null, null, error);
    }

}
