package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
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
 * When PR closed
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCloseEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Pull Request Closed";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRCloseEvent.class); //NOPMD

    @DataBoundConstructor
    public GitHubPRCloseEvent() {
    }

    @Override
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();
        final PrintStream logger = listener.getLogger();
        final GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();

        if (isNull(localPR)) {
            return null;
        }

        GitHubPRCause cause = null;

        // must be closed once
        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            logger.println(DISPLAY_NAME + ": state has changed (PR was closed)");
            cause = prDecisionContext.newCause("PR was closed", false);
        }

        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
