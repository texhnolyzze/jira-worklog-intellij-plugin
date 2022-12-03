package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Util {

    static final BigDecimal MINUTES_IN_HOUR = new BigDecimal(60);

    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("[A-Z\\d]+-\\d+");

    private static final Pattern JIRA_DURATION_PATTERN = Pattern.compile(
        "(?<hours1>(\\d+(([.,])\\d+)?h))\\s+(?<minutes1>(\\d+(([.,])\\d+)?m))|" +
        "(?<hours2>(\\d+(([.,])\\d+)?h))|" +
        "(?<minutes2>(\\d+(([.,])\\d+)?m))"
    );

    private Util() {
        throw new UnsupportedOperationException();
    }

    static String findJiraKey(final String str) {
        final Matcher matcher = JIRA_KEY_PATTERN.matcher(str);
        if (matcher.find()) {
            return str.substring(matcher.start(), matcher.end());
        }
        return null;
    }

    static boolean containsJiraKey(final String str) {
        return findJiraKey(str) != null;
    }

    static boolean isJiraKey(final String key) {
        return !StringUtils.isBlank(key) && JIRA_KEY_PATTERN.matcher(key).matches();
    }

    static String jiraKeyPrefix(@NotNull final String jiraKey) {
        return jiraKey.substring(0, jiraKey.indexOf('-'));
    }

    static String jiraKeyNumber(@NotNull final String jiraKey) {
        return jiraKey.substring(jiraKey.indexOf('-'));
    }

    static void showWorklogDialog(
        final @NotNull Project project,
        final @NotNull String jiraKeyContent
    ) {
        showWorklogDialog(project, jiraKeyContent, Util.getCurrentBranch(project), false);
    }

    static void showWorklogDialog(
        final @NotNull Project project,
        final @NotNull String jiraKeyContent,
        final @Nullable String branchName,
        final boolean pauseTimerAfterReset
    ) {
        if (StringUtils.isBlank(branchName)) {
            return;
        }
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, branchName, pauseTimerAfterReset);
        dialog.pack();
        dialog.afterPack();
        dialog.init(findJiraKey(jiraKeyContent));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    static String getCurrentBranch(final Project project) {
        final GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
        final List<GitRepository> repositories = manager.getRepositories();
        if (!repositories.isEmpty()) {
            final GitRepository repository = repositories.get(0);
            final GitLocalBranch localBranch = repository.getCurrentBranch();
            if (localBranch != null) {
                return localBranch.getName();
            }
        }
        return null;
    }

    static boolean isValidUrl(final String url) {
        try {
            new URL(url).toURI();
        } catch (URISyntaxException | MalformedURLException ignored) {
            return false;
        }
        return true;
    }

    static String formatAsJiraDuration(final Duration duration) {
        return duration.toHours() + "h " + duration.toMinutesPart() + "m";
    }

    static String minutesToJiraDuration(final Number minutes) {
        return formatAsJiraDuration(Duration.ofMinutes(minutes.intValue()));
    }

    static boolean isJiraDuration(String duration) {
        return parseJiraDuration(duration) != null;
    }

    static Duration parseJiraDuration(String duration) {
        if (duration == null) {
            return null;
        }
        duration = duration.replace(',', '.').strip();
        final Matcher matcher = JIRA_DURATION_PATTERN.matcher(duration);
        final boolean matches = matcher.matches();
        if (matches) {
            final BigDecimal hours;
            final BigDecimal minutes;
            final String hours1 = matcher.group("hours1");
            if (hours1 != null) {
                hours = new BigDecimal(hours1.substring(0, hours1.indexOf('h')));
                final String minutes1 = matcher.group("minutes1");
                minutes = new BigDecimal(minutes1.substring(0, minutes1.indexOf('m')));
            } else {
                final String hours2 = matcher.group("hours2");
                if (hours2 != null) {
                    hours = new BigDecimal(hours2.substring(0, hours2.indexOf('h')));
                    minutes = BigDecimal.ZERO;
                } else {
                    final String minutes2 = matcher.group("minutes2");
                    hours = BigDecimal.ZERO;
                    minutes = new BigDecimal(minutes2.substring(0, minutes2.indexOf('m')));
                }
            }
            final BigDecimal totalMinutes = hours.multiply(MINUTES_IN_HOUR).add(minutes);
            return Duration.ofMinutes(totalMinutes.intValue());
        }
        return null;
    }

    static String getRemoteRepositoryName(final Project project) {
        final GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
        final List<GitRepository> repositories = manager.getRepositories();
        if (!repositories.isEmpty()) {
            final GitRepository repository = repositories.get(0);
            final Collection<GitRemote> remotes = repository.getRemotes();
            if (!remotes.isEmpty()) {
                return remotes.iterator().next().getName();
            }
        }
        return null;
    }

    static String stripRemoteName(final String branch, final Project project) {
        final String remote = getRemoteRepositoryName(project);
        if (remote == null || !branch.startsWith(remote + "/")) {
            return branch;
        }
        return branch.substring((remote + "/").length());
    }

}
