package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.GHPullRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRDecisionContext extends GitHubDecisionContext<GitHubPREvent, GitHubPRCause> {
    private final GHPullRequest remotePR;
    private final GitHubPRPullRequest localPR;
    private final GitHubPRUserRestriction prUserRestriction;

    protected GitHubPRDecisionContext(@CheckForNull GHPullRequest remotePR,
                                      @CheckForNull GitHubPRPullRequest localPR,
                                      @CheckForNull GitHubPRUserRestriction prUserRestriction,
                                      GitHubSCMSource source,
                                      GitHubPRHandler prHandler,
                                      GitHubPRTrigger prTrigger,
                                      @Nonnull TaskListener listener) {
        super(listener, prTrigger, source, prHandler);
        this.remotePR = remotePR;
        this.localPR = localPR;
        this.prUserRestriction = prUserRestriction;
    }

    /**
     * remotePR current PR state fetched from GH
     * remotePRs are always existing on gh.
     */
    @Nonnull
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

    public static class Builder {
        private GHPullRequest remotePR = null;
        private GitHubPRPullRequest localPR = null;
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

        public Builder withListener(@Nonnull TaskListener listener) {
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

            return new GitHubPRDecisionContext(remotePR, localPR, prUserRestriction, source, prHandler, prTrigger, listener);
        }

    }

    @Nonnull
    public static Builder newGitHubPRDecisionContext() {
        return new Builder();
    }
}
