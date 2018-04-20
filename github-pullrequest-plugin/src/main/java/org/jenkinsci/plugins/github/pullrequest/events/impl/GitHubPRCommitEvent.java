package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

import static java.util.Objects.isNull;

/**
 * Triggers build when commit hash changed

 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommitEvent extends GitHubPREvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPROpenEvent.class);
    private static final String DISPLAY_NAME = "Commit changed";

    @DataBoundConstructor
    public GitHubPRCommitEvent() {
    }

    @Override
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();
        GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();

        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            //TODO check whether push to closed allowed?
            return null; // already closed, nothing to check
        }

        if (isNull(localPR)) { // new
            return null; // not interesting for this event
        }

        GitHubPRCause cause = null;

        GHCommitPointer head = remotePR.getHead();
        if (!localPR.getHeadSha().equals(head.getSha())) {
            LOGGER.debug("New commit. Sha: {} => {}", localPR.getHeadSha(), head.getSha());
            final PrintStream logger = listener.getLogger();
            logger.println(this.getClass().getSimpleName() + ": new commit found, sha " + head.getSha());
//            GHUser user = head.getUser();
            cause = prDecisionContext.newCause(DISPLAY_NAME, false);
        }

        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Nonnull
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
