package com.github.texhnolyzze.jiraworklogplugin.action;

import com.github.texhnolyzze.jiraworklogplugin.JiraWorklogPluginState;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class FlipShowWorklogFormOnEventAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            final boolean value = get().test(state);
            set().accept(state, !value);
        }
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        FlipActionUpdateUtils.update(e, get());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    protected abstract Predicate<JiraWorklogPluginState> get();
    protected abstract BiConsumer<JiraWorklogPluginState, Boolean> set();

    public static class OnExit extends FlipShowWorklogFormOnEventAction {

        @Override
        protected Predicate<JiraWorklogPluginState> get() {
            return JiraWorklogPluginState::isShowDialogOnExit;
        }

        @Override
        protected BiConsumer<JiraWorklogPluginState, Boolean> set() {
            return JiraWorklogPluginState::setShowDialogOnExit;
        }

    }

    public static class OnBranchChange extends FlipShowWorklogFormOnEventAction {

        @Override
        protected Predicate<JiraWorklogPluginState> get() {
            return JiraWorklogPluginState::isShowDialogOnBranchChange;
        }

        @Override
        protected BiConsumer<JiraWorklogPluginState, Boolean> set() {
            return JiraWorklogPluginState::setShowDialogOnBranchChange;
        }

    }

    public static class OnGitPush extends FlipShowWorklogFormOnEventAction {

        @Override
        protected Predicate<JiraWorklogPluginState> get() {
            return JiraWorklogPluginState::isShowDialogOnGitPush;
        }

        @Override
        protected BiConsumer<JiraWorklogPluginState, Boolean> set() {
            return JiraWorklogPluginState::setShowDialogOnGitPush;
        }

    }

}
