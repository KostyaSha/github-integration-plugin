package com.github.kostyasha.github.integration.branch.events;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.generic.GitHubBranchDecisionContext;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.kohsuke.github.GHEventPayload;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
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
     * @return cause object. null when no influence (other events will be checked.
     * If cause.isSkip() == true, then other checks wouldn't influence. And triggering for this branch will be skipped.
     * If cause.isSkip() == false, indicates that branch build should be run.
     */
    @CheckForNull
    public GitHubBranchCause check(@NonNull GitHubBranchDecisionContext context) throws IOException {
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
