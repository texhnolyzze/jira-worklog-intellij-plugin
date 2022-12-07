package com.github.texhnolyzze.jiraworklogplugin.workloggather;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class HowToDetermineWhenUserStartedWorkingActionGroup extends DefaultActionGroup {

    public HowToDetermineWhenUserStartedWorkingActionGroup() {
        getTemplatePresentation().setText(
            "How to Determine When You Started Working On Jira Issue By 'Started' And 'Time Spent'"
        );
    }

}
