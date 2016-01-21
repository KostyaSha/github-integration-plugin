package org.jenkinsci.plugins.github.pullrequest.events;

import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * Extension point for various GH events that triggers job
 *
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubPREvent extends AbstractDescribableImpl<GitHubPREvent> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPREvent.class);

    /**
     * indicates that PR was changed
     *
     * @param remotePR current PR state fetched from GH
     * @param localPR  PR state from last run saved in jenkins. null when not exist before
     * @return true if PR should be run
     */
    @CheckForNull
    public GitHubPRCause check(
            GitHubPRTrigger gitHubPRTrigger,
            GHPullRequest remotePR,
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
