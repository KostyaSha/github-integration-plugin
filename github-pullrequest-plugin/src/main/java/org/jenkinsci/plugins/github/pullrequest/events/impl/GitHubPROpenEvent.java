package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.kohsuke.github.GHIssueState.CLOSED;

/**
 * When PR opened or commits changed in it
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPROpenEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Pull Request Opened";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPROpenEvent.class);

    @DataBoundConstructor
    public GitHubPROpenEvent() {
    }

    @Override
    public GitHubPRCause check(GitHubPRHandler prHandler, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        return check(remotePR, localPR, listener);
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {

        return check(remotePR, localPR, listener);
    }

    private GitHubPRCause check(GHPullRequest remotePR,
                                @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        if (remotePR.getState() == CLOSED) {
            return null; // already closed, nothing to check
        }

        GitHubPRCause cause = null;
        String causeMessage = "PR opened";
        if (isNull(localPR)) { // new
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": state has changed (PR was opened)");
            cause = new GitHubPRCause(remotePR, causeMessage, false);
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
