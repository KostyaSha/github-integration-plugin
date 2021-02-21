package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.GHPullRequest;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRDecisionContext extends GitHubDecisionContext<GitHubPREvent, GitHubPRCause> {
    private final GHPullRequest remotePR;
    private final GitHubPRPullRequest localPR;
    private final GitHubPRUserRestriction prUserRestriction;
    private final GitHubPRRepository localRepo;

    protected GitHubPRDecisionContext(@CheckForNull GHPullRequest remotePR,
                                      @CheckForNull GitHubPRPullRequest localPR,
                                      @CheckForNull GitHubPRRepository localRepo,
                                      @CheckForNull GitHubPRUserRestriction prUserRestriction,
                                      GitHubSCMSource source,
                                      GitHubPRHandler prHandler,
                                      GitHubPRTrigger prTrigger,
                                      @NonNull TaskListener listener) {
        super(listener, prTrigger, source, prHandler);
        this.remotePR = remotePR;
        this.localPR = localPR;
        this.localRepo = localRepo;
        this.prUserRestriction = prUserRestriction;
    }

    @Deprecated
    protected GitHubPRDecisionContext(@CheckForNull GHPullRequest remotePR,
                                      @CheckForNull GitHubPRPullRequest localPR,
                                      @CheckForNull GitHubPRUserRestriction prUserRestriction,
                                      GitHubSCMSource source,
                                      GitHubPRHandler prHandler,
                                      GitHubPRTrigger prTrigger,
                                      @NonNull TaskListener listener) {
        this(remotePR, localPR, null, prUserRestriction, source, prHandler, prTrigger, listener);
    }

    /**
     * remotePR current PR state fetched from GH
     * remotePRs are always existing on gh.
     */
    @NonNull
    public GHPullRequest getRemotePR() {
        return remotePR;
    }

    /**
     * PR state from last run saved in jenkins. null when not exist before
     */
    @CheckForNull
    public GitHubPRPullRequest getLocalPR() {
        return localPR;
    }

    @CheckForNull
    public GitHubPRRepository getLocalRepo() {
        return localRepo;
    }

    @CheckForNull
    public GitHubPRUserRestriction getPrUserRestriction() {
        return prUserRestriction;
    }

    @Override
    public GitHubPRTrigger getTrigger() {
        return (GitHubPRTrigger) super.getTrigger();
    }

    @Override
    public GitHubPRHandler getHandler() {
        return (GitHubPRHandler) super.getHandler();
    }

    @CheckForNull
    @Deprecated
    public GitHubPRTrigger getPrTrigger() {
        return getTrigger();
    }

    @Override
    public GitHubPRCause checkEvent(GitHubPREvent event) throws IOException {
        return event.check(this);
    }

    @Override
    public GitHubPRCause newCause(String reason, boolean skip) {
        if (remotePR != null) {
            return new GitHubPRCause(remotePR, localRepo, reason, skip);
        }
        return new GitHubPRCause(localPR, null, localRepo, skip, reason);
    }

    public static class Builder {
        private GHPullRequest remotePR = null;
        private GitHubPRPullRequest localPR = null;
        private GitHubPRRepository localRepo = null;
        private TaskListener listener;
        private GitHubPRUserRestriction prUserRestriction = null;

        // depends on what job type it used
        private GitHubPRHandler prHandler = null;
        private GitHubPRTrigger prTrigger = null;
        private GitHubSCMSource source;


        public Builder() {
        }

        public Builder withRemotePR(@CheckForNull GHPullRequest remotePR) {
            this.remotePR = remotePR;
            return this;
        }

        public Builder withLocalPR(@CheckForNull GitHubPRPullRequest localPR) {
            this.localPR = localPR;
            return this;
        }

        public Builder withLocalRepo(GitHubPRRepository localRepo) {
            this.localRepo = localRepo;
            return this;
        }

        public Builder withListener(@NonNull TaskListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withPrHandler(@CheckForNull GitHubPRHandler prHandler) {
            this.prHandler = prHandler;
            return this;
        }

        // TODO abstract?
        public Builder withSCMSource(GitHubSCMSource source) {
            this.source = source;
            return this;
        }

        public Builder withPrTrigger(GitHubPRTrigger prTrigger) {
            this.prTrigger = prTrigger;
            return this;
        }

        public GitHubPRDecisionContext build() {
            if (isNull(prHandler)) {
                requireNonNull(prTrigger);
            } else {
                requireNonNull(prHandler);
                requireNonNull(source);
            }

            requireNonNull(listener);

            return new GitHubPRDecisionContext(remotePR, localPR, localRepo, prUserRestriction, source, prHandler, prTrigger, listener);
        }

    }

    @NonNull
    public static Builder newGitHubPRDecisionContext() {
        return new Builder();
    }
}
