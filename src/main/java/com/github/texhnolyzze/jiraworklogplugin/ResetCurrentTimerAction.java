package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.project.Project;

public class ResetCurrentTimerAction extends AbstractCurrentTimerAction {

    @Override
    protected void doAction(final Timer timer, final Project project) {
        timer.reset(project);
    }

}
