package com.github.texhnolyzze.jiraworklogplugin;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class JiraIssue implements Comparable<JiraIssue> {

    private final String key;
    private final String summary;
    private final String issueType;
    private final Integer timeEstimateSeconds;

    JiraIssue(
        final String key,
        final String summary,
        final String issueType,
        final Integer timeEstimateSeconds
    ) {
        this.key = key;
        this.summary = summary;
        this.issueType = issueType;
        this.timeEstimateSeconds = timeEstimateSeconds;
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
        final String myPrefix = Util.jiraKeyPrefix(key);
        final String otherPrefix = Util.jiraKeyPrefix(other.key);
        final int cmp = myPrefix.compareTo(otherPrefix);
        if (cmp == 0) {
            final int myNumber = Integer.parseInt(Util.jiraKeyNumber(key));
            final int otherNumber = Integer.parseInt(Util.jiraKeyNumber(other.key));
            return Integer.compare(myNumber, otherNumber);
        }
        return cmp;
    }

    static class Criteria {

        private String key;
        private String summary;
        private Collection<String> types;
        private Set<String> parents;

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

        public Collection<String> getTypes() {
            return types;
        }

        public void setTypes(final Collection<String> types) {
            this.types = types;
        }

        public Set<String> getParents() {
            return parents;
        }

        public void setParents(final Set<String> parents) {
            this.parents = parents;
        }

    }

}
