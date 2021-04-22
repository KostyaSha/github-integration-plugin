package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import hudson.model.TaskListener;
import org.kohsuke.github.GHRepository;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubDecisionContext<E, C extends GitHubCause<C>> {
    private final TaskListener listener;
    private final GitHubTrigger<?> trigger;
    private final GitHubSCMSource scmSource;
    private final GitHubHandler handler;

    protected GitHubDecisionContext(@NonNull TaskListener listener, GitHubTrigger<?> trigger, GitHubSCMSource scmSource, GitHubHandler handler) {
        this.listener = listener;
        this.trigger = trigger;
        this.scmSource = scmSource;
        this.handler = handler;
    }

    @NonNull
    public TaskListener getListener() {
        return listener;
    }

    @NonNull
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

    public abstract C newCause(String reason, boolean skip);
}
