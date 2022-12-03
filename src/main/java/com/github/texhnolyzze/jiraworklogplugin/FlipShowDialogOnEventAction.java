package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class FlipShowDialogOnEventAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            final boolean value = get().test(state);
            set().accept(state, !value);
        }
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
        super.update(e);
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final boolean show = get().test(state);
        if (show) {
            e.getPresentation().setIcon(AllIcons.Actions.Checked);
        } else {
            e.getPresentation().setIcon(null);
        }
    }

    protected abstract Predicate<JiraWorklogPluginState> get();
    protected abstract BiConsumer<JiraWorklogPluginState, Boolean> set();

    public static class OnExit extends FlipShowDialogOnEventAction {

        @Override
        protected Predicate<JiraWorklogPluginState> get() {
            return JiraWorklogPluginState::isShowDialogOnExit;
        }

        @Override
        protected BiConsumer<JiraWorklogPluginState, Boolean> set() {
            return JiraWorklogPluginState::setShowDialogOnExit;
        }

    }

    public static class OnBranchChange extends FlipShowDialogOnEventAction {

        @Override
        protected Predicate<JiraWorklogPluginState> get() {
            return JiraWorklogPluginState::isShowDialogOnBranchChange;
        }

        @Override
        protected BiConsumer<JiraWorklogPluginState, Boolean> set() {
            return JiraWorklogPluginState::setShowDialogOnBranchChange;
        }

    }

    public static class OnGitPush extends FlipShowDialogOnEventAction {

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
