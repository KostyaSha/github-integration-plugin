package com.github.kostyasha.github.integration.branch.events;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHEventPayload;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * Different "events" that may want trigger run for branch.
 *
 * @author Kanstantsin Shautsou
 * @see org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent
 */
public abstract class GitHubBranchEvent extends AbstractDescribableImpl<GitHubBranchEvent> implements ExtensionPoint {

    /**
     * indicates that branch was created
     *
     * @param gitHubBranchTrigger trigger itself.
     * @param remoteBranch        current branch state fetched from GH.
     * @param localBranch         branch state from last run saved in jenkins. null when not exist before.
     * @param localRepo           local repository state. Useful to extract repo URLs for example.
     * @return cause object. null when no influence (other events will be checked.
     * If cause.isSkip() == true, then other checks wouldn't influence. And triggering for this branch will be skipped.
     * If cause.isSkip() == false, indicates that branch build should be run.
     */
    @CheckForNull
    public GitHubBranchCause check(
            GitHubBranchTrigger gitHubBranchTrigger,
            GHBranch remoteBranch,
            @CheckForNull GitHubBranch localBranch,
            GitHubBranchRepository localRepo,
            TaskListener listener) throws IOException {
        return null;
    }

    /**
     * Check that is used for lightweight hooks (pure GitHub hooks).
     */
    public GitHubBranchCause checkHook(GitHubBranchTrigger githubTrigger,
                                       GHEventPayload payload,
                                       TaskListener listener) {
        return null;
    }

    @Override
    public GitHubBranchEventDescriptor getDescriptor() {
        return (GitHubBranchEventDescriptor) super.getDescriptor();
    }
}
