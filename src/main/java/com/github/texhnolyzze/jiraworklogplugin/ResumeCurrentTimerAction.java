package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.openapi.project.Project;

public class ResumeCurrentTimerAction extends AbstractCurrentTimerAction {

    @Override
    protected void doAction(final Timer timer, final Project project) {
        timer.resume(project);
    }

}
