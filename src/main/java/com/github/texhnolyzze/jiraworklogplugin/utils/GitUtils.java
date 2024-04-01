package com.github.texhnolyzze.jiraworklogplugin.utils;

import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Collection;
import java.util.List;

public final class GitUtils {

    private GitUtils() {
        throw new UnsupportedOperationException();
    }

    public static String getCurrentBranch(final Project project) {
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

    public static String getRemoteRepositoryName(final Project project) {
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

    public static String stripRemoteName(final String branch, final Project project) {
        final String remote = getRemoteRepositoryName(project);
        if (remote == null || !branch.startsWith(remote + "/")) {
            return branch;
        }
        return branch.substring((remote + "/").length());
    }

}
