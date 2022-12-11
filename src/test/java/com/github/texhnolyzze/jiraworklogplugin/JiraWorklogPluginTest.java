package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.workloggather.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ReflectionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.*;
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
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
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
        final TodayWorklogSummaryResponse response = new TodayWorklogSummaryResponse(
            Duration.ofHours(5),
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
            ),
            null
        );
        final Duration timeSpentViaExternal = (Duration) findTimeSpentViaExternalWorklogs.invoke(dialog, response);
        assertEquals(Duration.ofMinutes(20), timeSpentViaExternal);
    }

}
