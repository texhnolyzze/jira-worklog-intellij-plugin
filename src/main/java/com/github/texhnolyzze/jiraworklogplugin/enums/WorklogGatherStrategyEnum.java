package com.github.texhnolyzze.jiraworklogplugin.enums;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;
import com.github.texhnolyzze.jiraworklogplugin.workloggather.RestApiV2WorklogGatherStrategy;
import com.github.texhnolyzze.jiraworklogplugin.workloggather.TimesheetGadgetWorklogGatherStrategy;
import com.github.texhnolyzze.jiraworklogplugin.workloggather.WorklogGatherStrategy;

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
    };

    public abstract WorklogGatherStrategy create(final JiraClient client);

}
