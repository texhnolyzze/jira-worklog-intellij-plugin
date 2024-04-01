package com.github.texhnolyzze.jiraworklogplugin.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JiraKeyUtils {

    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("[A-Z\\d]+-\\d+");

    private JiraKeyUtils() {
        throw new UnsupportedOperationException();
    }


    public static String findJiraKey(final String str) {
        final Matcher matcher = JIRA_KEY_PATTERN.matcher(str);
        if (matcher.find()) {
            return str.substring(matcher.start(), matcher.end());
        }
        return null;
    }

    public static boolean isJiraKey(final String key) {
        return !StringUtils.isBlank(key) && JIRA_KEY_PATTERN.matcher(key).matches();
    }

    public static String jiraKeyPrefix(@NotNull final String jiraKey) {
        return jiraKey.substring(0, jiraKey.indexOf('-'));
    }

    public static String jiraKeyNumber(@NotNull final String jiraKey) {
        return jiraKey.substring(jiraKey.indexOf('-'));
    }

}
