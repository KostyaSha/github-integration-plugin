package org.jenkinsci.plugins.github.pullrequest.events;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequestReview;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * Extension point for various GH PR Review events that may trigger run.
 *
 * @author Nicola Covallero
 */
public abstract class GitHubPRReviewEvent extends AbstractDescribableImpl<GitHubPREvent> implements ExtensionPoint {

    /**
     * indicates that PR was changed
     *
     * @param remotePR current PR state fetched from GH
     * @param localPR  PR state from last run saved in jenkins. null when not exist before
     * @return cause object. null when no influence (other events will be checked.
     * If cause.isSkip() == true, then other checks wouldn't influence. And triggering for this branch will be skipped.
     * If cause.isSkip() == false, indicates that branch build should be run.
     */
    @CheckForNull
    public GitHubPRCause check(
            GitHubPRTrigger gitHubPRTrigger,
            GHPullRequestReview remotePR,
            @CheckForNull GitHubPRPullRequest localPR,
            TaskListener listener) throws IOException {
        return null;
    }

    /**
     * Check that is used for lightweight hooks (pure GitHub hooks).
     */
    public GitHubPRCause checkHook(GitHubPRTrigger gitHubPRTrigger,
                                   GHEventPayload payload,
                                   TaskListener listener) {
        return null;
    }

    @Override
    public GitHubPREventDescriptor getDescriptor() {
        return (GitHubPREventDescriptor) super.getDescriptor();
    }

}
