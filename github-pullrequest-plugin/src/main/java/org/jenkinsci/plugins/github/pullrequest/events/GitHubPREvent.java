package org.jenkinsci.plugins.github.pullrequest.events;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHEventPayload;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Extension point for various GH PR events that may trigger run.
 *
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubPREvent extends AbstractDescribableImpl<GitHubPREvent> implements ExtensionPoint {

    /**
     * indicates that PR was changed
     *
     * @return cause object. null when no influence (other events will be checked.
     * If cause.isSkip() == true, then other checks wouldn't influence. And triggering for this branch will be skipped.
     * If cause.isSkip() == false, indicates that branch build should be run.
     */
    @CheckForNull
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
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
