package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Triggers build when commit hash changed

 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommitEvent extends GitHubPREvent {
    private static final Logger LOGGER = Logger.getLogger(GitHubPROpenEvent.class.getName());
    static final String causeMessage = "Commit changed";

    @DataBoundConstructor
    public GitHubPRCommitEvent() {
    }

    @Override
    public GitHubPRCause isStateChanged(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR, GitHubPRPullRequest localPR) throws IOException {
        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            //TODO check whether push to closed allowed?
            return null; // already closed, nothing to check
        }

        if (localPR == null) { // new
            return null; // not interesting for this event
        }

        GitHubPRCause cause = null;

        GHCommitPointer head = remotePR.getHead();
        if (!localPR.getHeadSha().equals(head.getSha())) {
            LOGGER.log(Level.FINE, "New commit. Sha: {0} => {1}", new Object[]{localPR.getHeadSha(), head.getSha()});
            GHUser user = head.getUser();
            cause = new GitHubPRCause(remotePR, remotePR.getUser(), causeMessage, user.getName(), user.getEmail());
        }

        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public final String getDisplayName() {
            return causeMessage;
        }
    }
}
