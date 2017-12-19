package com.github.kostyasha.github.integration.generic;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchDecisionContext extends GitHubDecisionContext {

    private final GHBranch remoteBranch;
    private final GitHubBranch localBranch;
    private final GitHubBranchRepository localRepo;
    private final GitHubBranchHandler branchHandler;
    private final GitHubSCMSource scmSource;
    private final GitHubBranchTrigger branchTrigger;

    public GitHubBranchDecisionContext(GHBranch remoteBranch, GitHubBranch localBranch,
                                       @Nonnull GitHubBranchRepository localRepo,
                                       GitHubBranchHandler branchHandler,
                                       GitHubSCMSource scmSource,
                                       GitHubBranchTrigger branchTrigger, TaskListener listener) {
        super(listener);
        this.remoteBranch = remoteBranch;
        this.localBranch = localBranch;
        this.localRepo = localRepo;
        this.branchHandler = branchHandler;
        this.scmSource = scmSource;
        this.branchTrigger = branchTrigger;
    }

    /**
     * @return current branch state fetched from GH.
     */
    @CheckForNull
    public GHBranch getRemoteBranch() {
        return remoteBranch;
    }

    /**
     * @return branch state from last run saved in jenkins. null when not exist before.
     */
    @CheckForNull
    public GitHubBranch getLocalBranch() {
        return localBranch;
    }

    /**
     * @return local repository state. Useful to extract repo URLs for example.
     */
    @Nonnull
    public GitHubBranchRepository getLocalRepo() {
        return localRepo;
    }

    public GitHubBranchHandler getBranchHandler() {
        return branchHandler;
    }

    public GitHubSCMSource getScmSource() {
        return scmSource;
    }

    public GitHubBranchTrigger getBranchTrigger() {
        return branchTrigger;
    }

    public static class Builder {
        private GHBranch remoteBranch = null;
        private GitHubBranch localBranch = null;
        private TaskListener listener;

        // depends on what job type it used
        private GitHubBranchHandler branchHandler = null;
        private GitHubBranchTrigger branchTrigger = null;
        private GitHubSCMSource scmSource;

        private GitHubBranchRepository localRepo;

        public Builder() {
        }

        public Builder withRemoteBranch(@CheckForNull GHBranch remoteBranch) {
            this.remoteBranch = remoteBranch;
            return this;
        }

        public Builder withLocalBranch(@CheckForNull GitHubBranch localBranch) {
            this.localBranch = localBranch;
            return this;
        }

        public Builder withLocalRepo(GitHubBranchRepository localRepo) {
            this.localRepo = localRepo;
            return this;
        }

        public Builder withBranchTrigger(GitHubBranchTrigger branchTrigger) {
            this.branchTrigger = branchTrigger;
            return this;
        }

        public Builder withListener(@Nonnull TaskListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder withBranchHandler(@CheckForNull GitHubBranchHandler branchHandler) {
            this.branchHandler = branchHandler;
            return this;
        }

        // TODO abstract?
        public Builder withSCMSource(GitHubSCMSource scmSource) {
            this.scmSource = scmSource;
            return this;
        }


        public GitHubBranchDecisionContext build() {
            if (isNull(branchHandler)) {
                requireNonNull(branchTrigger);
            } else {
                requireNonNull(branchHandler);
                requireNonNull(scmSource);
            }

            requireNonNull(listener);

            return new GitHubBranchDecisionContext(remoteBranch,
                    localBranch, localRepo,
                    branchHandler, scmSource, branchTrigger, listener);
        }

    }

    @Nonnull
    public static Builder newGitHubBranchDecisionContext() {
        return new Builder();
    }
}
