package com.github.texhnolyzze.jiraworklogplugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.texhnolyzze.jiraworklogplugin.enums.AdjustEstimate;
import com.github.texhnolyzze.jiraworklogplugin.enums.AuthorizeWith;
import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.enums.WorklogGatherStrategyEnum;
import com.github.texhnolyzze.jiraworklogplugin.jirarequest.AddWorklogRequest;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.*;
import com.github.texhnolyzze.jiraworklogplugin.utils.EmailUtils;
import com.google.common.net.HttpHeaders;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.net.HTTPMethod;
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

import static com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue.LEAVE_AS_IS;
import static com.github.texhnolyzze.jiraworklogplugin.utils.JiraDurationUtils.formatAsJiraDuration;
import static com.github.texhnolyzze.jiraworklogplugin.utils.Utils.OBJECT_MAPPER;

public class JiraClient {

    private static final Logger logger = Logger.getInstance(JiraClient.class);

    private static final DateTimeFormatter ADD_WORKLOG_STARTED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000+0000'");

    public static final String JIRA_RESPONSE_CODE = "Jira response code: ";

    public static final String DEFAULT_FIELDS = "key,summary,issuetype,timeestimate,assignee,status";

    private static final String APPLICATION_JSON = "application/json";

    private final HttpClient httpClient;
    private final Project project;

    private final Map<String, AuthorizeWith> authorizeWithMap = new HashMap<>();

    JiraClient(final Project project) {
        this.project = project;
        httpClient = HttpClient.newBuilder().
            version(HttpClient.Version.HTTP_1_1).
            followRedirects(HttpClient.Redirect.NEVER).
            connectTimeout(Duration.ofSeconds(20)).
            build();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public static JiraClient getInstance(final Project project) {
        return project.getService(JiraClient.class);
    }

    @SuppressWarnings("java:S3358")
    AddWorklogResponse addWorklog(
        @NotNull final String jiraUrl,
        @NotNull final String email,
        @NotNull final String password,
        @NotNull final JiraIssue issue,
        @NotNull final Duration timeSpent,
        @Nullable final String comment,
        @Nullable final AdjustEstimate adjustEstimate,
        @Nullable final Duration adjustmentDuration,
        @NotNull final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        try {
            final URI uri = addWorklogUri(jiraUrl, issue, adjustEstimate, adjustmentDuration);
            final HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(uri)
                            .header(HttpHeaders.AUTHORIZATION, getAuthorization(email, password))
                            .header("Content-Type", APPLICATION_JSON)
                            .method(HTTPMethod.POST.name(), addWorklogBody(timeSpent, comment, how))
                            .build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 201 && !responseIsJson(response)) {
                return AddWorklogResponse.error(JIRA_RESPONSE_CODE + response.statusCode());
            }
            if (responseIsJson(response)) {
                //noinspection unchecked
                final @Nullable AddWorklogResponse errorMessages = tryErrorMessages(
                    OBJECT_MAPPER.readValue(response.body(), Map.class),
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
            final String email,
            final String password,
            final JiraIssue.Criteria criteria,
            final String... fields
    ) {
        try {
            final String jql = URLEncoder.encode(
                toJql(criteria),
                StandardCharsets.UTF_8
            );
            final HttpRequest request =
                HttpRequest
                    .newBuilder()
                    .uri(
                        new URI(
                            jiraUrl +
                                (jiraUrl.endsWith("/") ? "" : "/") +
                                "rest/api/2/search?" +
                                "jql=" + jql + "&" +
                                "fields=" + (fields.length == 0 ? DEFAULT_FIELDS : String.join(",", fields))
                        )
                    )
                    .header(HttpHeaders.AUTHORIZATION, getAuthorization(email, password))
                    .build();
            final HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200 && !responseIsJson(response)) {
                return FindJiraIssuesResponse.error(JIRA_RESPONSE_CODE + response.statusCode());
            }
            //noinspection unchecked
            final Map<String, Object> map = OBJECT_MAPPER.readValue(response.body(), Map.class);
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

    public TodayWorklogSummaryResponse getTodayWorklogSummary(
            final String jiraUrl,
            final String email,
            final String password,
            final WorklogGatherStrategyEnum gatherType,
            final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        if (authorizeWithMap.get(email) == null) {
            probeAuth(jiraUrl, email, password);
        }
        return gatherType.create(this).get(jiraUrl, email, password, how);
    }

    private void probeAuth(final String jiraUrl, final String email, final String password) {
        if (!email.contains("@")) {
            authorizeWithMap.put(email, AuthorizeWith.USERNAME);
        }
        final URI uri = URI.create(
                jiraUrl + (jiraUrl.endsWith("/") ? "" : "/") + "rest/api/2/search?maxResults=0&fields=key"
        );
        final String emailAuth = "Basic " + Base64.getEncoder().encodeToString(
                (email + ":" + password).getBytes(StandardCharsets.UTF_8)
        );
        if (authProbeSuccess(uri, emailAuth)) {
            authorizeWithMap.put(email, AuthorizeWith.EMAIL);
        } else {
            final String usernameAuth = "Basic " + Base64.getEncoder().encodeToString(
                    (EmailUtils.getUsername(email) + ":" + password).getBytes(StandardCharsets.UTF_8)
            );
            if (authProbeSuccess(uri, usernameAuth)) {
                authorizeWithMap.put(email, AuthorizeWith.USERNAME);
            } else {
                logger.warn("Can't authorize user with neither email nor username");
                authorizeWithMap.put(email, AuthorizeWith.EMAIL);
            }
        }
    }

    private boolean authProbeSuccess(final URI uri, final String authorization) {
        final HttpRequest build = HttpRequest
                .newBuilder()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .build();
        try {
            final HttpResponse<Void> response = httpClient.send(build, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error searching Jira issues", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    public FindJiraWorklogsResponse findWorklogs(
        @NotNull final String jiraUrl,
        @NotNull final String email,
        @NotNull final String password,
        @NotNull final String issue,
        @NotNull final HowToDetermineWhenUserStartedWorkingOnIssue how
    ) {
        try {
            final HttpResponse<InputStream> response = httpClient.send(
                HttpRequest
                        .newBuilder()
                        .uri(
                                new URI(
                                        jiraUrl +
                                                (jiraUrl.endsWith("/") ? "" : "/") +
                                                "rest/api/2/issue/" + issue + "/worklog"
                                )
                        )
                        .header(HttpHeaders.AUTHORIZATION, getAuthorization(email, password))
                        .build(),
                HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() != 200) {
                return new FindJiraWorklogsResponse(
                    null,
                    "Jira response code " + response.statusCode() + " when searching worklogs of issue " + issue
                );
            }
            //noinspection unchecked
            final Map<String, Object> map = OBJECT_MAPPER.readValue(response.body(), Map.class);
            return new FindJiraWorklogsResponse(convertWorklogs(issue, map, how), null);
        } catch (IOException | InterruptedException | URISyntaxException e) {
            logger.error("Error getting worklogs for issue " + issue, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    public String getAuthorization(final String email, final String password) {
        final AuthorizeWith authorizeWith = authorizeWithMap.getOrDefault(email, AuthorizeWith.EMAIL);
        final String auth;
        if (authorizeWith == AuthorizeWith.EMAIL) {
            auth = email;
        } else {
            auth = EmailUtils.getUsername(email);
        }
        return "Basic " + Base64.getEncoder().encodeToString(
            (auth + ":" + password).getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean responseIsJson(final HttpResponse<?> response) {
        return response.headers().allValues("content-type").stream().anyMatch(s -> s.contains(APPLICATION_JSON));
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

    @NotNull
    private static URI addWorklogUri(
            final @NotNull String jiraUrl,
            final @NotNull JiraIssue issue,
            final @Nullable AdjustEstimate adjustEstimate,
            final @Nullable Duration adjustmentDuration
    ) throws URISyntaxException {
        final String adjustEstimatePart;
        if (adjustEstimate == null) {
            adjustEstimatePart = "";
        } else {
            final String adjustmentDurationPart =
                    adjustEstimate.getAdjustmentDurationQueryParameter() == null
                    ? ""
                    : "&" + adjustEstimate.getAdjustmentDurationQueryParameter() + "=" + URLEncoder.encode(
                            formatAsJiraDuration(Objects.requireNonNull(adjustmentDuration)),
                            StandardCharsets.UTF_8
                    );
            adjustEstimatePart = "adjustEstimate=" + adjustEstimate.getId() + adjustmentDurationPart;
        }
        return new URI(
                jiraUrl +
                        (jiraUrl.endsWith("/") ? "" : "/") +
                        "rest/api/2/issue/" + issue.getKey() + "/worklog?" +
                        adjustEstimatePart
        );
    }

    @NotNull
    private HttpRequest.BodyPublisher addWorklogBody(
            final @NotNull Duration timeSpent,
            final @Nullable String comment,
            final @NotNull HowToDetermineWhenUserStartedWorkingOnIssue how
    ) throws JsonProcessingException {
        return HttpRequest.BodyPublishers.ofString(
                OBJECT_MAPPER.writeValueAsString(
                        new AddWorklogRequest(
                                project.getName(),
                                Duration.ofMinutes(timeSpent.toMinutes()).toSeconds(),
                                comment,
                                LocalDateTime
                                        .now(Clock.systemUTC())
                                        .minus(how == LEAVE_AS_IS ? timeSpent : Duration.ZERO)
                                        .format(ADD_WORKLOG_STARTED_FORMAT)
                        )
                ),
                StandardCharsets.UTF_8
        );
    }

    @SuppressWarnings("unchecked")
    private NavigableSet<JiraIssue> convert(final List<Map<String, Object>> issues) {
        final NavigableSet<JiraIssue> result = new TreeSet<>(Comparator.naturalOrder());
        for (final Map<String, Object> issue : issues) {
            final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            final Map<String, Object> issueType = fields == null ? null : (Map<String, Object>) fields.get("issuetype");
            result.add(
                new JiraIssue(
                    (String) issue.get("key"),
                    fields == null ? null : (String) fields.get("summary"),
                    issueType == null ? null : (String) issueType.get("name"),
                    fields == null || fields.get("timeestimate") == null ?
                        null :
                        ((Number) fields.get("timeestimate")).intValue(),
                    fields == null || fields.get("assignee") == null ?
                        null :
                        (String) ((Map<String, Object>) fields.get("assignee")).get("key"),
                    fields == null || fields.get("status") == null ?
                        null :
                        new JiraIssue.Status(
                            (String) ((Map<String, Object>) fields.get("status")).get("id"),
                            (String) ((Map<String, Object>) fields.get("status")).get("name")
                        )
                )
            );
        }
        return result;
    }

    private String toJql(final JiraIssue.Criteria criteria) {
        final List<String> conditions = new ArrayList<>();
        if (!StringUtils.isBlank(criteria.getKey())) {
            conditions.add("key=" + escapeJql(criteria.getKey().strip()));
        }
        if (!StringUtils.isBlank(criteria.getSummary())) {
            conditions.add("summary~" + escapeJql(criteria.getSummary()));
        }
        if (!StringUtils.isBlank(criteria.getWorklogAuthor())) {
            conditions.add("worklogAuthor=" + escapeJql(criteria.getWorklogAuthor()));
        }
        if (criteria.getWorklogDate() != null) {
            conditions.add("worklogDate=" + criteria.getWorklogDate());
        }
        return String.join(" and ", conditions);
    }

    private String escapeJql(final String str) {
        final StringBuilder res = new StringBuilder(str.length() + 2);
        res.append("\"");
        for (int i = 0, len = str.length(); i < len; i++) {
            final char c = str.charAt(i);
            if (c == '"') {
                res.append("\\\"");
            } else if (c == '\\') {
                res.append("\\\\");
            } else {
                res.append(c);
            }
        }
        res.append("\"");
        return res.toString();
    }

    private List<JiraWorklog> convertWorklogs(
        final String issue,
        final Map<String, Object> map,
        final HowToDetermineWhenUserStartedWorkingOnIssue how
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
                    author == null ? null : (String) author.get("emailAddress"),
                    how
                )
            );
        }
        return result;
    }

    public AuthorizeWith getAuthorizeWith(final String email) {
        return authorizeWithMap.getOrDefault(email, AuthorizeWith.EMAIL);
    }

}
