package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.TodayWorklogSummaryResponse;
import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.github.texhnolyzze.jiraworklogplugin.utils.JiraProfilesUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.PluginCredentialsUtils;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ReflectionUtil;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

public class JiraWorklogPluginTest extends BasePlatformTestCase {

    public void testFindIntersections() throws InvocationTargetException, IllegalAccessException {
        final Project project = myFixture.getProject();
        final String branch = "story/JIRA-ISSUE-1";
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, branch);
        final Method findTimeSpentViaExternalWorklogs = ReflectionUtil.getDeclaredMethod(
            JiraWorklogDialog.class,
            "findTimeSpentViaExternalWorklogs",
            TodayWorklogSummaryResponse.class
        );
        final LocalDate date = LocalDate.of(2022, 12, 7);
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            state.appendUnitOfWork(
                new UnitOfWork(
                    branch,
                    ZonedDateTime.of(
                        date,
                        LocalTime.of(11, 10),
                        ZoneId.systemDefault()
                    ),
                    Duration.ofMinutes(10)
                )
            );
            state.appendUnitOfWork(
                new UnitOfWork(
                    branch,
                    ZonedDateTime.of(
                        date,
                        LocalTime.of(11, 40),
                        ZoneId.systemDefault()
                    ),
                    Duration.ofMinutes(30)
                )
            );
            state.appendUnitOfWork(
                new UnitOfWork(
                    branch,
                    ZonedDateTime.of(
                        date,
                        LocalTime.of(12, 40),
                        ZoneId.systemDefault()
                    ),
                    Duration.ofMinutes(10)
                )
            );
        }
        final TodayWorklogSummaryResponse response = TodayWorklogSummaryResponse.success(
            List.of(
                new JiraWorklog(
                    ZonedDateTime.of(
                        date,
                        LocalTime.of(12, 0),
                        ZoneId.systemDefault()
                    ),
                    Duration.ofMinutes(30),
                    "abc",
                    null,
                    null,
                    HowToDetermineWhenUserStartedWorkingOnIssue.LEAVE_AS_IS
                ),
                new JiraWorklog(
                    ZonedDateTime.of(
                        date,
                        LocalTime.of(12, 30),
                        ZoneId.systemDefault()
                    ),
                    Duration.ofMinutes(30),
                    "abc",
                    null,
                    null,
                    HowToDetermineWhenUserStartedWorkingOnIssue.LEAVE_AS_IS
                )
            )
        );
        final Duration timeSpentViaExternal = (Duration) findTimeSpentViaExternalWorklogs.invoke(dialog, response);
        assertEquals(Duration.ofMinutes(20), timeSpentViaExternal);
    }

    public void testTodayTotalAfterLogging() throws Exception {
        final Project project = myFixture.getProject();
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, "story/ABC-1");
        final Field todayLoggedDuration = JiraWorklogDialog.class.getDeclaredField("todayLoggedDuration");
        todayLoggedDuration.setAccessible(true);
        todayLoggedDuration.set(dialog, Duration.ofHours(3));
        final Field timeSpentField = JiraWorklogDialog.class.getDeclaredField("timeSpent");
        timeSpentField.setAccessible(true);
        ((JTextField) timeSpentField.get(dialog)).setText("2h 30m");
        final Method update = ReflectionUtil.getDeclaredMethod(JiraWorklogDialog.class, "updateTodayTotalAfterLogging");
        update.invoke(dialog);
        final Field resultField = JiraWorklogDialog.class.getDeclaredField("todayTotalAfterLogging");
        resultField.setAccessible(true);
        assertEquals("5h 30m", ((JTextField) resultField.get(dialog)).getText());
    }

    public void testTodayTotalAfterLogging_whenNoLoggedTimeYet_shouldBeEmpty() throws Exception {
        final Project project = myFixture.getProject();
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, "story/ABC-1");
        final Method update = ReflectionUtil.getDeclaredMethod(JiraWorklogDialog.class, "updateTodayTotalAfterLogging");
        update.invoke(dialog);
        final Field resultField = JiraWorklogDialog.class.getDeclaredField("todayTotalAfterLogging");
        resultField.setAccessible(true);
        final JTextField field = (JTextField) resultField.get(dialog);
        assertTrue(field.getText() == null || field.getText().isEmpty());
    }

    public void testTransferTimeToCurrentBranch() throws Exception {
        final Project project = myFixture.getProject();
        final String sourceBranch = "story/ABC-1";
        final String targetBranch = "story/ABC-2";
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            state.appendUnitOfWork(
                new UnitOfWork(
                    sourceBranch,
                    ZonedDateTime.now(ZoneId.systemDefault()).minus(Duration.ofMinutes(30)),
                    Duration.ofMinutes(30)
                )
            );
            setTotal(state.getTimer(sourceBranch, project), Duration.ofMinutes(15));
        }
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, sourceBranch, targetBranch);
        final Field transferButtonField = JiraWorklogDialog.class.getDeclaredField("transferToButton");
        transferButtonField.setAccessible(true);
        final JButton transferButton = (JButton) transferButtonField.get(dialog);
        assertTrue(transferButton.isVisible());
        assertTrue(transferButton.getText().contains(targetBranch));
        final Method onTransfer = ReflectionUtil.getDeclaredMethod(JiraWorklogDialog.class, "onTransfer");
        onTransfer.invoke(dialog);
        synchronized (state) {
            assertEquals(Duration.ofMinutes(15), state.getTimer(targetBranch, project).toDuration());
            assertEquals(Duration.ZERO, state.getTimer(sourceBranch, project).toDuration());
            assertTrue(
                state.getTimeSeries().stream().allMatch(work -> targetBranch.equals(work.getBranch()))
            );
        }
    }

    public void testTransferButton_whenNoTransferTarget_shouldBeHidden() throws Exception {
        final Project project = myFixture.getProject();
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, "story/ABC-1");
        final Field transferButtonField = JiraWorklogDialog.class.getDeclaredField("transferToButton");
        transferButtonField.setAccessible(true);
        final JButton transferButton = (JButton) transferButtonField.get(dialog);
        assertFalse(transferButton.isVisible());
    }

    private static void setTotal(final Timer timer, final Duration duration) throws Exception {
        final Field total = Timer.class.getDeclaredField("total");
        total.setAccessible(true);
        total.setLong(timer, duration.toNanos());
    }

    public void testAutoDialogsButton_togglesAllWhenToShowDialogFlags() throws Exception {
        final Project project = myFixture.getProject();
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final JiraWorklogDialog dialog = new JiraWorklogDialog(project, "story/ABC-1");
        final Field buttonField = JiraWorklogDialog.class.getDeclaredField("autoDialogsButton");
        buttonField.setAccessible(true);
        final JButton button = (JButton) buttonField.get(dialog);
        assertTrue(button.getText().startsWith("Don't show"));
        button.doClick();
        synchronized (state) {
            assertFalse(state.isShowDialogOnExit());
            assertFalse(state.isShowDialogOnBranchChange());
            assertFalse(state.isShowDialogOnGitPush());
        }
        assertEquals("Show dialogs automatically", button.getText());
        button.doClick();
        synchronized (state) {
            assertTrue(state.isShowDialogOnExit());
            assertTrue(state.isShowDialogOnBranchChange());
            assertTrue(state.isShowDialogOnGitPush());
        }
        assertEquals("Don't show dialogs automatically", button.getText());
    }

    public void testKnownUrls_mruOrdering() {
        PropertiesComponent.getInstance().unsetValue(JiraProfilesUtils.KNOWN_URLS_PROPERTY);
        try {
            JiraProfilesUtils.addKnownUrl("https://first.atlassian.net");
            JiraProfilesUtils.addKnownUrl("https://second.atlassian.net");
            JiraProfilesUtils.addKnownUrl("https://first.atlassian.net");
            assertEquals(
                List.of("https://first.atlassian.net", "https://second.atlassian.net"),
                JiraProfilesUtils.getKnownUrls()
            );
        } finally {
            PropertiesComponent.getInstance().unsetValue(JiraProfilesUtils.KNOWN_URLS_PROPERTY);
        }
    }

    public void testDialog_knownUrlSelected_prefillsCredentialsFromKeychain() throws Exception {
        final Project project = myFixture.getProject();
        final String url = "https://known-urls-test.atlassian.net";
        final CredentialAttributes attributes = PluginCredentialsUtils.getCredentialAttributes(url);
        try {
            PasswordSafe.getInstance().set(attributes, new Credentials("user@example.com", "api-token-123"));
            JiraProfilesUtils.addKnownUrl(url);
            final JiraWorklogDialog dialog = new JiraWorklogDialog(project, "story/ABC-1");
            final Field jiraUrlField = JiraWorklogDialog.class.getDeclaredField("jiraUrl");
            jiraUrlField.setAccessible(true);
            @SuppressWarnings("unchecked")
            final JComboBox<String> jiraUrl = (JComboBox<String>) jiraUrlField.get(dialog);
            final List<String> items = new ArrayList<>();
            for (int i = 0; i < jiraUrl.getItemCount(); i++) {
                items.add(jiraUrl.getItemAt(i));
            }
            assertTrue(items.contains(url));
            jiraUrl.setSelectedItem(url);
            final Field emailField = JiraWorklogDialog.class.getDeclaredField("email");
            emailField.setAccessible(true);
            final Field passwordField = JiraWorklogDialog.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            assertEquals("user@example.com", ((JTextField) emailField.get(dialog)).getText());
            assertEquals("api-token-123", ((JPasswordField) passwordField.get(dialog)).getText());
        } finally {
            // no remove() in the 2023.2 CredentialStore API - overwrite with empty password
            PasswordSafe.getInstance().set(attributes, new Credentials("user@example.com", ""));
            PropertiesComponent.getInstance().unsetValue(JiraProfilesUtils.KNOWN_URLS_PROPERTY);
        }
    }

}
