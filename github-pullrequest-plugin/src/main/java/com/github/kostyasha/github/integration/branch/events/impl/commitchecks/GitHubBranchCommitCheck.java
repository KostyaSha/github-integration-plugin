package com.github.kostyasha.github.integration.branch.events.impl.commitchecks;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @see GitHubBranchCommitEvent
 */
public abstract class GitHubBranchCommitCheck extends AbstractDescribableImpl<GitHubBranchCommitCheck>
        implements ExtensionPoint {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchCommitCheck.class);

    @Override
    public GitHubBranchCommitCheckDescriptor getDescriptor() {
        return (GitHubBranchCommitCheckDescriptor) super.getDescriptor();
    }

    /**
     * Check used to determine if some associated commit property, such as the commit message, should prevent a build from being triggered.
     *
     * @param remoteBranch current branch state from GH.
     * @param localRepo    local repository state.
     * @param commits      commits commits that occurred between the last known local hash and the current remote.
     * @return <code>GitHubBranchCause</code> instance indicating if the build should be skipped, <code>null</code> otherwise.
     */
    public abstract GitHubBranchCause check(GHBranch remoteBranch, GitHubBranchRepository localRepo, Commit[] commits);

    /**
     * Check used to determine if some associated commit property, such as the commit message, should prevent a build from being triggered.
     * <p>
     * This method is called when the repository is not yet known to the plugin.
     * </p>
     *
     * @param remoteBranch current branch state from GH.
     * @param localRepo    local repository state.
     * @param commit       last commit in the remote repository.
     * @return <code>GitHubBranchCause</code> instance indicating if the build should be skipped, <code>null</code> otherwise.
     */
    public GitHubBranchCause check(GHBranch remoteBranch, GitHubBranchRepository localRepo, GHCommit commit) {
        try {
            return doCheck(remoteBranch, localRepo, commit);
        } catch (IOException e) {
            LOG.error("Failed to check commit for hash [{}]", commit.getSHA1(), e);
            return new GitHubBranchCause(remoteBranch, localRepo, e.getMessage(), true);
        }
    }

    protected abstract GitHubBranchCause doCheck(GHBranch remoteBranch, GitHubBranchRepository localRepo, GHCommit commit)
            throws IOException;
}
