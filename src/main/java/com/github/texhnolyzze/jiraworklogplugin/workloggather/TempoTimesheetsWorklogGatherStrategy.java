package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.github.texhnolyzze.jiraworklogplugin.JiraClient;
import com.github.texhnolyzze.jiraworklogplugin.JiraWorklog;
import com.github.texhnolyzze.jiraworklogplugin.TodayWorklogSummaryResponse;
import com.google.common.base.Preconditions;
import com.google.common.net.HttpHeaders;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.texhnolyzze.jiraworklogplugin.Util.OBJECT_MAPPER;

public class TempoTimesheetsWorklogGatherStrategy extends WorklogGatherStrategy {

    private static final DateTimeFormatter TEMPO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss.SSS"
    );

    protected TempoTimesheetsWorklogGatherStrategy(final JiraClient client) {
        super(client);
    }

    @Override
    public TodayWorklogSummaryResponse get(
        final String jiraUrl,
        final String username,
        final String password,
        final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        try {
            final LocalDate now = LocalDate.now(ZoneId.systemDefault());
            final Request request = new Request(
                now,
                username
            );
            final HttpResponse<InputStream> response = client.getHttpClient().send(
                HttpRequest.newBuilder().
                    uri(
                        new URI(
                            jiraUrl +
                            (jiraUrl.endsWith("/") ? "" : "/") +
                            "rest/tempo-timesheets/4/worklogs/search"
                        )
                    ).
                    method(
                        "POST",
                        HttpRequest.BodyPublishers.ofString(
                            OBJECT_MAPPER.writeValueAsString(
                                request
                            )
                        )
                    ).
                    header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()).
                    header(HttpHeaders.AUTHORIZATION, client.getAuthorization(username, password)).
                    build(),
                HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() != 200) {
                return TodayWorklogSummaryResponse.error(
                    "tempo-timesheets returned " + response.statusCode()
                );
            }
            //noinspection unchecked
            final List<Map<String, Object>> list = OBJECT_MAPPER.readValue(response.body(), List.class);
            final List<JiraWorklog> worklogs = new ArrayList<>(list.size());
            for (final Map<String, Object> worklog : list) {
                final String startDate = (String) worklog.get("startDate");
                final Number timeSpentSeconds = (Number) worklog.get("timeSpentSeconds");
                //noinspection unchecked
                final Map<String, Object> issue = (Map<String, Object>) worklog.get("issue");
                Preconditions.checkState(startDate != null, "startDate is empty");
                Preconditions.checkState(timeSpentSeconds != null, "timeSpentSeconds is empty");
                worklogs.add(
                    new JiraWorklog(
                        LocalDateTime.parse(startDate, TEMPO_DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()),
                        Duration.ofSeconds(timeSpentSeconds.longValue()),
                        issue == null ? null : (String) issue.get("key"),
                        (String) worklog.get("comment"),
                        (String) worklog.get("workerKey"),
                        how
                    )
                );
            }
            return TodayWorklogSummaryResponse.success(worklogs);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return TodayWorklogSummaryResponse.error("tempo timesheet error: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private static class Request {

        private final String from;
        private final String to;
        private final List<String> worker;

        private Request(
            final LocalDate date,
            final String worker
        ) {
            this.from = date.format(DateTimeFormatter.ISO_DATE);
            this.to = date.format(DateTimeFormatter.ISO_DATE);
            this.worker = Collections.singletonList(worker);
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public List<String> getWorker() {
            return worker;
        }

    }

}
