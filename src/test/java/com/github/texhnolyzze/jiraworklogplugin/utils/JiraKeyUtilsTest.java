package com.github.texhnolyzze.jiraworklogplugin.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraKeyUtilsTest {

    @Test
    public void givenUppercaseBranch_whenFindJiraKey_shouldFindKey() {
        assertThat(JiraKeyUtils.findJiraKey("feature/ABC-123")).isEqualTo("ABC-123");
    }

    @Test
    public void givenLowercaseBranch_whenFindJiraKey_shouldFindUppercasedKey() {
        assertThat(JiraKeyUtils.findJiraKey("feature/abc-123")).isEqualTo("ABC-123");
    }

    @Test
    public void givenMixedCaseBranch_whenFindJiraKey_shouldFindUppercasedKey() {
        assertThat(JiraKeyUtils.findJiraKey("feature/AbC-123")).isEqualTo("ABC-123");
    }

    @Test
    public void givenLowercaseBranchWithoutPrefix_whenFindJiraKey_shouldFindUppercasedKey() {
        assertThat(JiraKeyUtils.findJiraKey("abc-123")).isEqualTo("ABC-123");
    }

    @Test
    public void givenCommitMessageWithLowercaseKey_whenFindJiraKey_shouldFindUppercasedKey() {
        assertThat(JiraKeyUtils.findJiraKey("fix bug in abc-123 handling")).isEqualTo("ABC-123");
    }

    @Test
    public void givenStringWithoutJiraKey_whenFindJiraKey_shouldReturnNull() {
        assertThat(JiraKeyUtils.findJiraKey("feature/no-key-here")).isNull();
    }

    @Test
    public void givenUppercaseKey_whenIsJiraKey_shouldReturnTrue() {
        assertThat(JiraKeyUtils.isJiraKey("ABC-123")).isTrue();
    }

    @Test
    public void givenLowercaseKey_whenIsJiraKey_shouldReturnTrue() {
        assertThat(JiraKeyUtils.isJiraKey("abc-123")).isTrue();
    }

    @Test
    public void givenNonKey_whenIsJiraKey_shouldReturnFalse() {
        assertThat(JiraKeyUtils.isJiraKey("feature/no-key-here")).isFalse();
        assertThat(JiraKeyUtils.isJiraKey("")).isFalse();
    }

}
