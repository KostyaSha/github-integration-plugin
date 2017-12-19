package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import hudson.model.Item;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHPullRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRDecisionContext extends GitHubDecisionContext {
    private final GHPullRequest remotePR;
    private final GitHubPRPullRequest localPR;
    private final GitHubPRUserRestriction prUserRestriction;

    private final GitHubSCMSource source;
    // depends on what job type it used
    private final GitHubPRHandler prHandler;
    private final GitHubPRTrigger prTrigger;


    protected GitHubPRDecisionContext(@CheckForNull GHPullRequest remotePR,
                                      @CheckForNull GitHubPRPullRequest localPR,
                                      @CheckForNull GitHubPRUserRestriction prUserRestriction,
                                      GitHubSCMSource source,
                                      GitHubPRHandler prHandler,
                                      GitHubPRTrigger prTrigger,
                                      @Nonnull TaskListener listener) {
        super(listener);
        this.remotePR = remotePR;
        this.localPR = localPR;
        this.prUserRestriction = prUserRestriction;
        this.source = source;
        this.prHandler = prHandler;
        this.prTrigger = prTrigger;
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


    @CheckForNull
    public GitHubPRHandler getPrHandler() {
        return prHandler;
    }

    @CheckForNull
    public GitHubSCMSource getSource() {
        return source;
    }

    @CheckForNull
    public GitHubPRTrigger getPrTrigger() {
        return prTrigger;
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
