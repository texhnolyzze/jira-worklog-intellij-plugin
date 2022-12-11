package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;

public enum WorklogGatherStrategyEnum {

    REST_API_V2() {
        @Override
        public WorklogGatherStrategy create(final JiraClient client) {
            return new RestApiV2WorklogGatherStrategy(client);
        }
    },
    TIMESHEET_GADGET {
        @Override
        public WorklogGatherStrategy create(final JiraClient client) {
            return new TimesheetGadgetWorklogGatherStrategy(client);
        }
    },
    TEMPO_TIMESHEETS {
        @Override
        public WorklogGatherStrategy create(final JiraClient client) {
            return new TempoTimesheetsWorklogGatherStrategy(client);
        }
    };

    public abstract WorklogGatherStrategy create(final JiraClient client);

}
