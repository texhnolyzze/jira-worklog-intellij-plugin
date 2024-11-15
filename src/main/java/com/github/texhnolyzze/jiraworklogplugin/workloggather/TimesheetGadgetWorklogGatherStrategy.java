package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;
import com.github.texhnolyzze.jiraworklogplugin.JiraWorklog;
import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.TodayWorklogSummaryResponse;
import com.google.common.net.HttpHeaders;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.texhnolyzze.jiraworklogplugin.utils.Utils.OBJECT_MAPPER;

public class TimesheetGadgetWorklogGatherStrategy extends WorklogGatherStrategy {

    private static final Logger logger = Logger.getInstance(TimesheetGadgetWorklogGatherStrategy.class);

    public TimesheetGadgetWorklogGatherStrategy(final JiraClient client) {
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
            final LocalDate now = LocalDate.now(ZoneId.systemDefault());
            final HttpResponse<InputStream> response = client.getHttpClient().send(
                HttpRequest.newBuilder().
                    uri(
                        URI.create(
                            jiraUrl +
                            (jiraUrl.endsWith("/") ? "" : "/") +
                            "rest/timesheet-gadget/1.0/raw-timesheet.json?" +
                            "targetUser=" + email + "&" +
                            "startDate=" + now + "&" +
                            "endDate=" + now
                        )
                    ).
                    header(HttpHeaders.AUTHORIZATION, client.getAuthorization(email, password, jiraUrl)).
                    build(),
                HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() != 200) {
                return client.getErrorResponse(response, TodayWorklogSummaryResponse::error);
            }
            //noinspection unchecked
            final Map<String, Object> map = OBJECT_MAPPER.readValue(response.body(), Map.class);
            //noinspection unchecked
            final List<Map<String, Object>> worklogs = (List<Map<String, Object>>) map.get("worklog");
            final ArrayList<JiraWorklog> convertedWorklogs = new ArrayList<>(worklogs.size());
            for (final Map<String, Object> worklog : worklogs) {
                final String key = (String) worklog.get("key");
                //noinspection unchecked
                final List<Map<String, Object>> entries = (List<Map<String, Object>>) worklog.get("entries");
                for (final Map<String, Object> entry : entries) {
                    final String comment = (String) entry.get("comment");
                    final Number timeSpent = (Number) entry.get("timeSpent");
                    final Number startDate = (Number) entry.get("startDate");
                    convertedWorklogs.add(
                        new JiraWorklog(
                            Instant.ofEpochMilli(startDate.longValue()).atZone(ZoneId.systemDefault()),
                            Duration.ofSeconds(timeSpent.longValue()),
                            key,
                            comment,
                            email,
                            how
                        )
                    );
                }
            }
            return TodayWorklogSummaryResponse.success(convertedWorklogs);
        } catch (final Exception e) {
            logger.error("Error getting today worklog summary", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return TodayWorklogSummaryResponse.error("timesheet-gadget error: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

}
