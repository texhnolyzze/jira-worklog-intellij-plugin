package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import java.time.Duration;
import java.time.ZonedDateTime;

public enum HowToDetermineWhenUserStartedWorkingOnIssue {

    /**
     * 'Started' is just time of worklog creation, so actually user started working 'Started' - 'Time Spent' ago.
     * Most users do like that
     */
    SUBTRACT_TIME_SPENT() {
        @Override
        public ZonedDateTime determine(final ZonedDateTime jiraStarted, final Duration timeSpent) {
            return jiraStarted.minus(timeSpent);
        }
    },
    /**
     * 'Started' is actually started, and not the time of worklog creation (minority of users)
     */
    LEAVE_AS_IS {
        @Override
        public ZonedDateTime determine(final ZonedDateTime jiraStarted, final Duration timeSpent) {
            return jiraStarted;
        }
    };

    public abstract ZonedDateTime determine(final ZonedDateTime jiraStarted, final Duration timeSpent);

}
