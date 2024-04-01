package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;
import com.github.texhnolyzze.jiraworklogplugin.JiraWorklog;
import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.FindJiraIssuesResponse;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.FindJiraWorklogsResponse;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.JiraIssue;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.TodayWorklogSummaryResponse;
import com.github.texhnolyzze.jiraworklogplugin.utils.EmailUtils;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class RestApiV2WorklogGatherStrategy extends WorklogGatherStrategy {

    private static final Logger logger = Logger.getInstance(RestApiV2WorklogGatherStrategy.class);

    public RestApiV2WorklogGatherStrategy(final JiraClient client) {
        super(client);
    }

    @Override
    public TodayWorklogSummaryResponse get(
            final String jiraUrl,
            final String email,
            final String password,
            final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        try {
            final JiraIssue.Criteria criteria = new JiraIssue.Criteria();
            criteria.setWorklogDate(LocalDate.now(ZoneId.systemDefault()));
            criteria.setWorklogAuthor(email);
            final FindJiraIssuesResponse issues = client.findIssues(jiraUrl, email, password, criteria, "key");
            if (issues.getError() != null && !issues.getError().isBlank()) {
                return TodayWorklogSummaryResponse.error(issues.getError());
            }
            final List<JiraWorklog> worklogs = new ArrayList<>();
            final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            final ZonedDateTime dayStart = now.withHour(0).withMinute(0).withSecond(0);
            final ZonedDateTime dayEnd = now.withHour(23).withMinute(59).withSecond(59);
            for (final JiraIssue issue : issues.getIssues()) {
                final FindJiraWorklogsResponse response = client.findWorklogs(
                    jiraUrl,
                    email,
                    password,
                    issue.getKey(),
                    how
                );
                if (response.getError() != null && !response.getError().isBlank()) {
                    return TodayWorklogSummaryResponse.error(response.getError());
                }
                final ListIterator<JiraWorklog> iterator = response.getWorklogs().listIterator(response.getWorklogs().size());
                while (iterator.hasPrevious()) {
                    final JiraWorklog worklog = iterator.previous();
                    final String worklogEmail = worklog.getAuthorEmailAddress();
                    if (EmailUtils.sameUser(email, worklogEmail)) {
                        final ZonedDateTime worklogStart = how.determine(worklog.getStartTime(), worklog.getTimeSpent());
                        if (dayStart.compareTo(worklogStart) <= 0 && worklogStart.compareTo(dayEnd) <= 0) {
                            worklogs.add(worklog);
                        } else {
                            break; // we can safely skip remaining worklogs since they are sorted by 'Started'
                        }
                    }
                }
            }
            return TodayWorklogSummaryResponse.success(worklogs);
        } catch (final Exception e) {
            logger.error("Error getting today worklog summary", e);
            return TodayWorklogSummaryResponse.error("rest api v2 error: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

}
