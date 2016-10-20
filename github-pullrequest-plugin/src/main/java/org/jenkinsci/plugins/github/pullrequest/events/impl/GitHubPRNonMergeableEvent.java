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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Event to skip PRs that can't be merged.
 *
 * @author Alina Karpovich
 */
public class GitHubPRNonMergeableEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Not mergeable";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRNonMergeableEvent.class);

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
            listener.error(DISPLAY_NAME + ": can't get mergeable status {}", e);
            LOGGER.warn("Can't get mergeable status: {}", e);
            mergeable = false;
        }
        mergeable = mergeable != null ? mergeable : false;

        if (!mergeable) {
            return new GitHubPRCause(remotePR, DISPLAY_NAME, isSkip());
        }

        return null;
    }

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
