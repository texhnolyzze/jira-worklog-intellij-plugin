package com.github.texhnolyzze.jiraworklogplugin.jiraresponse;

import com.github.texhnolyzze.jiraworklogplugin.utils.JiraKeyUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Objects;

public class JiraIssue implements Comparable<JiraIssue> {

    private final String key;
    private final String summary;
    private final String issueType;
    private final Integer timeEstimateSeconds;
    private final String assignee;
    private final Status status;

    public JiraIssue(
        final String key,
        final String summary,
        final String issueType,
        final Integer timeEstimateSeconds,
        final String assignee,
        final Status status
    ) {
        this.key = key;
        this.summary = summary;
        this.issueType = issueType;
        this.timeEstimateSeconds = timeEstimateSeconds;
        this.assignee = assignee;
        this.status = status;
    }

    public String getKey() {
        return key;
    }

    public String prettySummary() {
        return "(" + issueType + ") " + summary;
    }

    public Integer getTimeEstimateSeconds() {
        return timeEstimateSeconds;
    }

    public String getAssignee() {
        return assignee;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof JiraIssue)) return false;
        final JiraIssue jiraIssue = (JiraIssue) o;
        return key.equals(jiraIssue.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public int compareTo(@NotNull final JiraIssue other) {
        final String myPrefix = JiraKeyUtils.jiraKeyPrefix(key);
        final String otherPrefix = JiraKeyUtils.jiraKeyPrefix(other.key);
        final int cmp = myPrefix.compareTo(otherPrefix);
        if (cmp == 0) {
            final int myNumber = Integer.parseInt(JiraKeyUtils.jiraKeyNumber(key));
            final int otherNumber = Integer.parseInt(JiraKeyUtils.jiraKeyNumber(other.key));
            return Integer.compare(myNumber, otherNumber);
        }
        return cmp;
    }

    public static class Status {

        private final String id;
        private final String name;

        public Status(final String id, final String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

    }

    public static class Criteria {

        private String key;
        private String summary;
        private LocalDate worklogDate;
        private String worklogAuthor;

        public String getKey() {
            return key;
        }

        public void setKey(final String key) {
            this.key = key;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(final String summary) {
            this.summary = summary;
        }

        public LocalDate getWorklogDate() {
            return worklogDate;
        }

        public void setWorklogDate(final LocalDate worklogDate) {
            this.worklogDate = worklogDate;
        }

        public String getWorklogAuthor() {
            return worklogAuthor;
        }

        public void setWorklogAuthor(final String worklogAuthor) {
            this.worklogAuthor = worklogAuthor;
        }

        @Override
        public String toString() {
            return "Criteria{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                ", worklogDate=" + worklogDate +
                ", worklogAuthor='" + worklogAuthor + '\'' +
                '}';
        }

    }

}
