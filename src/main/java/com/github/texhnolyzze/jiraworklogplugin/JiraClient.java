package com.github.texhnolyzze.jiraworklogplugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.texhnolyzze.jiraworklogplugin.workloggather.WorklogGatherStrategy;
import com.github.texhnolyzze.jiraworklogplugin.workloggather.WorklogGatherStrategyEnum;
import com.google.common.net.HttpHeaders;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.net.HTTPMethod;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.texhnolyzze.jiraworklogplugin.Util.formatAsJiraDuration;

public class JiraClient {

    private static final Logger logger = Logger.getInstance(JiraClient.class);

    private static final DateTimeFormatter ADD_WORKLOG_STARTED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000+0000'");

    public static final String JIRA_RESPONSE_CODE = "Jira response code: ";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    JiraClient() {
        objectMapper = new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        httpClient = HttpClient.newBuilder().
            version(HttpClient.Version.HTTP_1_1).
            followRedirects(HttpClient.Redirect.NEVER).
            connectTimeout(Duration.ofSeconds(20)).
            build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static JiraClient getInstance(final Project project) {
        return project.getService(JiraClient.class);
    }

    @SuppressWarnings("java:S3358")
    public AddWorklogResponse addWorklog(
        @NotNull final String jiraUrl,
        @NotNull final String username,
        @NotNull final String password,
        @NotNull final JiraIssue issue,
        @NotNull final Duration timeSpent,
        @Nullable final String comment,
        @Nullable final AdjustEstimate adjustEstimate,
        @Nullable final Duration adjustmentDuration,
        @NotNull final WorklogGatherStrategy.HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        try {
            final HttpRequest request = HttpRequest.newBuilder().uri(
                new URI(
                    jiraUrl +
                    (jiraUrl.endsWith("/") ? "" : "/") +
                    "rest/api/2/issue/" + issue.getKey() + "/worklog?" +
                    (
                        adjustEstimate == null ?
                        "" :
                        "adjustEstimate=" + adjustEstimate.getId() +
                        (
                            adjustEstimate.getAdjustmentDurationQueryParameter() == null ?
                            "" :
                            "&" + adjustEstimate.getAdjustmentDurationQueryParameter() + "=" + URLEncoder.encode(
                                formatAsJiraDuration(Objects.requireNonNull(adjustmentDuration)),
                                StandardCharsets.UTF_8
                            )
                        )
                    )
                )
            ).
            header(HttpHeaders.AUTHORIZATION, getAuthorization(username, password)).
            header("Content-Type", "application/json").
            method(
                HTTPMethod.POST.name(),
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(
                        new AddWorklogRequest(
                            Duration.ofMinutes(timeSpent.toMinutes()).toSeconds(),
                            comment,
                            LocalDateTime.now(Clock.systemUTC()).minus(
                                how == WorklogGatherStrategy.HowToDetermineWhenUserStartedWorkingOnIssue.LEAVE_AS_IS ?
                                timeSpent :
                                Duration.ZERO
                            ).format(ADD_WORKLOG_STARTED_FORMAT)
                        )
                    ),
                    StandardCharsets.UTF_8
                )
            ).
            build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 201 && !responseIsJson(response)) {
                return AddWorklogResponse.error(JIRA_RESPONSE_CODE + response.statusCode());
            }
            if (responseIsJson(response)) {
                //noinspection unchecked
                final @Nullable AddWorklogResponse errorMessages = tryErrorMessages(
                    objectMapper.readValue(response.body(), Map.class),
                    AddWorklogResponse::error
                );
                if (errorMessages != null) {
                    return errorMessages;
                }
            }
            return AddWorklogResponse.success();
        } catch (final Exception e) {
            logger.error("Error adding worklog", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return AddWorklogResponse.error(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public FindJiraIssuesResponse findIssues(
        final String jiraUrl,
        final String username,
        final String password,
        final JiraIssue.Criteria criteria,
        final String... fields
    ) {
        try {
            final String jql = URLEncoder.encode(
                toJql(criteria),
                StandardCharsets.UTF_8
            );
            final HttpRequest request = HttpRequest.newBuilder().uri(
                new URI(
                    jiraUrl +
                    (jiraUrl.endsWith("/") ? "" : "/") +
                    "rest/api/2/search?" +
                    "jql=" + jql + "&" +
                    "fields=" + (fields.length == 0 ? "key,summary,issuetype,timeestimate" : String.join(",", fields))
                )
            ).
            header(HttpHeaders.AUTHORIZATION, getAuthorization(username, password)).
            build();
            final HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200 && !responseIsJson(response)) {
                return FindJiraIssuesResponse.error(JIRA_RESPONSE_CODE + response.statusCode());
            }
            //noinspection unchecked
            final Map<String, Object> map = objectMapper.readValue(response.body(), Map.class);
            final FindJiraIssuesResponse errorMessages = tryErrorMessages(map, FindJiraIssuesResponse::error);
            if (errorMessages != null) {
                return errorMessages;
            }
            //noinspection unchecked
            final List<Map<String, Object>> issues = (List<Map<String, Object>>) map.get("issues");
            return FindJiraIssuesResponse.success(convert(issues));
        } catch (final Exception e) {
            logger.error("Error searching Jira issues", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return FindJiraIssuesResponse.error(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private boolean responseIsJson(final HttpResponse<?> response) {
        return response.headers().allValues("content-type").stream().anyMatch(s -> s.contains("application/json"));
    }

    @Nullable
    private <T> T tryErrorMessages(
        final Map<String, Object> map,
        final Function<String, ? extends T> errorResponseFactory
    ) {
        if (map.get("errorMessages") != null) {
            //noinspection unchecked
            final List<String> errorMessages = (List<String>) map.get("errorMessages");
            if (!errorMessages.isEmpty() && !StringUtils.isBlank(errorMessages.get(0))) {
                return errorResponseFactory.apply(errorMessages.get(0));
            }
        }
        return null;
    }

    private NavigableSet<JiraIssue> convert(final List<Map<String, Object>> issues) {
        final NavigableSet<JiraIssue> result = new TreeSet<>(Comparator.naturalOrder());
        for (final Map<String, Object> issue : issues) {
            //noinspection unchecked
            final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            //noinspection unchecked
            final Map<String, Object> issueType = fields == null ? null : (Map<String, Object>) fields.get("issuetype");
            result.add(
                new JiraIssue(
                    (String) issue.get("key"),
                    fields == null ? null : (String) fields.get("summary"),
                    issueType == null ? null : (String) issueType.get("name"),
                    fields == null || fields.get("timeestimate") == null ? null : ((Number) fields.get("timeestimate")).intValue()
                )
            );
        }
        return result;
    }

    private String toJql(final JiraIssue.Criteria criteria) {
        final List<String> conditions = new ArrayList<>();
        if (!StringUtils.isBlank(criteria.getKey())) {
            conditions.add("key=" + criteria.getKey().strip());
        }
        if (!CollectionUtils.isEmpty(criteria.getParents())) {
            conditions.add("parent in " + criteria.getParents().stream().collect(Collectors.joining(",", "(", ")")));
        }
        if (!StringUtils.isBlank(criteria.getSummary())) {
            conditions.add("summary~'" + criteria.getSummary() + "'");
        }
        if (!CollectionUtils.isEmpty(criteria.getTypes())) {
            conditions.add(
                "type in " + criteria.getTypes().stream().map(
                    type -> "'" + type + "'"
                ).collect(Collectors.joining(",", "(", ")"))
            );
        }
        if (!StringUtils.isBlank(criteria.getWorklogAuthor())) {
            conditions.add("worklogAuthor=" + criteria.getWorklogAuthor());
        }
        if (criteria.getWorklogDate() != null) {
            conditions.add("worklogDate=" + criteria.getWorklogDate());
        }
        return String.join(" and ", conditions);
    }

    public TodayWorklogSummaryResponse getTodayWorklogSummary(
        final String jiraUrl,
        final String username,
        final String password,
        final WorklogGatherStrategyEnum gatherType,
        final WorklogGatherStrategy.HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        return gatherType.create(this).get(jiraUrl, username, password, how);
    }

    public FindJiraWorklogsResponse findWorklogs(
        @NotNull final String jiraUrl,
        @NotNull final String username,
        @NotNull final String password,
        @NotNull final String issue,
        @NotNull final WorklogGatherStrategy.HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        try {
            final HttpResponse<InputStream> response = httpClient.send(
                HttpRequest.newBuilder().
                    uri(
                        new URI(
                            jiraUrl +
                            (jiraUrl.endsWith("/") ? "" : "/") +
                            "rest/api/2/issue/" + issue + "/worklog"
                        )
                    ).
                    header(HttpHeaders.AUTHORIZATION, getAuthorization(username, password)).
                    build(),
                HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() != 200) {
                return new FindJiraWorklogsResponse(
                    null,
                    "Jira response code " + response.statusCode() + " when searching worklogs of issue " + issue
                );
            }
            //noinspection unchecked
            final Map<String, Object> map = objectMapper.readValue(response.body(), Map.class);
            return new FindJiraWorklogsResponse(convertWorklogs(issue, map, how), null);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            logger.error("Error getting worklogs for issue " + issue, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(e);
        }
    }

    private List<JiraWorklog> convertWorklogs(
        final String issue,
        final Map<String, Object> map,
        final WorklogGatherStrategy.HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        //noinspection unchecked
        final List<Map<String, Object>> worklogs = (List<Map<String, Object>>) map.get("worklogs");
        final List<JiraWorklog> result = new ArrayList<>(worklogs.size());
        for (final Map<String, Object> worklog : worklogs) {
            final String started = (String) worklog.get("started");
            final Number timeSpentSeconds = (Number) worklog.get("timeSpentSeconds");
            //noinspection unchecked
            final Map<String, Object> author = (Map<String, Object>) worklog.get("author");
            result.add(
                new JiraWorklog(
                    started == null ?
                    null :
                    OffsetDateTime.parse(started.substring(0, 19) + "+" + started.substring(24, 26) + ":" + started.substring(26)).toZonedDateTime(),
                    timeSpentSeconds == null ? null : Duration.ofSeconds(timeSpentSeconds.longValue()),
                    issue,
                    (String) worklog.get("comment"),
                    author == null ? null : (String) author.get("key"),
                    how
                )
            );
        }
        return result;
    }

    @NotNull
    public String getAuthorization(final String username, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
            (username + ":" + password).getBytes(StandardCharsets.UTF_8)
        );
    }

}
