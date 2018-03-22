package com.github.kostyasha.github.integration.multibranch.handler;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMSourceCriteria;

public class GitHubSourceContext {

    private final GitHubSCMSource source;
    private final SCMHeadObserver observer;
    private final SCMSourceCriteria criteria;
    private final SCMHeadEvent<?> scmHeadEvent;
    private final GitHubRepo localRepo;
    private final GHRepository remoteRepo;
    private final TaskListener listener;

    public GitHubSourceContext( //
            @Nonnull GitHubSCMSource source, //
            @Nonnull SCMHeadObserver observer, //
            @Nonnull SCMSourceCriteria criteria, //
            @Nullable SCMHeadEvent<?> scmHeadEvent, //
            @Nonnull GitHubRepo localRepo, //
            @Nonnull GHRepository remoteRepo, //
            @Nonnull TaskListener listener) {
        this.source = source;
        this.observer = observer;
        this.criteria = criteria;
        this.scmHeadEvent = scmHeadEvent;
        this.localRepo = localRepo;
        this.remoteRepo = remoteRepo;
        this.listener = listener;
    }

    public @Nonnull GitHubSCMSource getSource() {
        return source;
    }

    public @Nonnull SCMHeadObserver getObserver() {
        return observer;
    }

    public SCMHeadEvent<?> getScmHeadEvent() {
        return scmHeadEvent;
    }

    public @Nonnull GitHubRepo getLocalRepo() {
        return localRepo;
    }

    public @Nonnull GHRepository getRemoteRepo() {
        return remoteRepo;
    }

    public @Nonnull TaskListener getListener() {
        return listener;
    }

    public GitHub getGitHub() {
        return source.getRepoProvider().getGitHub(source);
    }

    public boolean checkCriteria(@Nonnull GitHubSCMHead head, @Nonnull GitHubSCMRevision revision) throws IOException {
        if (criteria != null) {
            listener.getLogger().println("Checking " + head.getPronoun());
            if (!criteria.isHead(source.newProbe(head, revision), listener)) {
                listener.getLogger().println("  Didn't meet criteria\n");
                return false;
            }
            listener.getLogger().println("  Met criteria\n");
        }
        return true;
    }

}
