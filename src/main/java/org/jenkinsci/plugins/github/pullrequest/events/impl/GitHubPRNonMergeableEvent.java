package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event to skip PRs that can't be merged.
 *
 * @author Alina Karpovich
 */
public class GitHubPRNonMergeableEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Not mergeable";
    private final static Logger LOGGER = Logger.getLogger(GitHubPRNonMergeableEvent.class.getName());

    private boolean skip = true;

    @DataBoundConstructor
    public GitHubPRNonMergeableEvent(boolean skip) {
        this.skip = skip;
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        Boolean mergeable;
        try {
            mergeable = remotePR.getMergeable();
        } catch (IOException e) {
            logger.println(DISPLAY_NAME + ": can't get mergeable status");
            LOGGER.log(Level.WARNING, "Can't get mergeable status: {}", e.getMessage());
            mergeable = false;
        }
        mergeable = mergeable != null ? mergeable : false;

        if (!mergeable) {
            return new GitHubPRCause(remotePR, DISPLAY_NAME, isSkip());
        }

        return null;
    }

    @Override
    public boolean isSkip() {
        return skip;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
