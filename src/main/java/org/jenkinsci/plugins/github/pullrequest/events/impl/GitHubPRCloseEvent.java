package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * When PR closed
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCloseEvent extends GitHubPREvent {
    private final static Logger LOGGER = Logger.getLogger(GitHubPRCloseEvent.class.getName());

    @DataBoundConstructor
    public GitHubPRCloseEvent() {
    }

    @Override
    public GitHubPRCause isStateChanged(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR, GitHubPRPullRequest localPR) throws IOException {
        if (localPR == null) {
            return null;
        }

        GitHubPRCause cause = null;

        // must be closed once
        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            cause = new GitHubPRCause(remotePR, null, "PR was closed", null, null);
        }

        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return "Pull Request Closed";
        }
    }
}
