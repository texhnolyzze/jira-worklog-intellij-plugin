package com.github.texhnolyzze.jiraworklogplugin;

class TodayWorklogSummaryResponse {

    private final Integer minutesSpent;
    private final String error;

    TodayWorklogSummaryResponse(
        final Integer minutesSpent,
        final String error
    ) {
        this.minutesSpent = minutesSpent;
        this.error = error;
    }

    public String getRemainedToLogPretty() {
        return Util.minutesToJiraDuration(8 * 60 - minutesSpent);
    }

    public String getSpentPretty() {
        return Util.minutesToJiraDuration(minutesSpent);
    }

    public String getError() {
        return error;
    }

    static TodayWorklogSummaryResponse success(final Integer minutesSpent) {
        return new TodayWorklogSummaryResponse(minutesSpent, null);
    }

    static TodayWorklogSummaryResponse error(final String error) {
        return new TodayWorklogSummaryResponse(null, error);
    }

}
