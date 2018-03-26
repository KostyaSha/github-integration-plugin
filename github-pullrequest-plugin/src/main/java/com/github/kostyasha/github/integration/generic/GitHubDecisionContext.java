package com.github.kostyasha.github.integration.generic;

import hudson.model.TaskListener;

import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.github.GHRepository;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubDecisionContext<E, C extends GitHubCause<C>> {
    private final TaskListener listener;
    private final GitHubTrigger<?> trigger;
    private final GitHubSCMSource scmSource;
    private final GitHubHandler handler;

    protected GitHubDecisionContext(@Nonnull TaskListener listener, GitHubTrigger<?> trigger, GitHubSCMSource scmSource, GitHubHandler handler) {
        this.listener = listener;
        this.trigger = trigger;
        this.scmSource = scmSource;
        this.handler = handler;
    }

    @Nonnull
    public TaskListener getListener() {
        return listener;
    }

    @Nonnull
    public GHRepository getRemoteRepository() throws IOException {
        GHRepository repo = null;
        if (scmSource != null) {
            repo = scmSource.getRemoteRepo();
        } else if (trigger != null) {
            repo = trigger.getRemoteRepository();
        }
        if (repo == null) {
            throw new IOException("No remote repository");
        }
        return repo;
    }

    @CheckForNull
    public GitHubSCMSource getScmSource() {
        return scmSource;
    }

    @CheckForNull
    public GitHubHandler getHandler() {
        return handler;
    }

    @CheckForNull
    public GitHubTrigger<?> getTrigger() {
        return trigger;
    }

    public abstract C checkEvent(E event) throws IOException;
}
