package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Event to skip PRs that can't be merged.
 *
 * @author Alina Karpovich
 */
public class GitHubPRSkipNonMergeableEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Skip not mergeable";
    private final static Logger LOGGER = Logger.getLogger(GitHubPRSkipNonMergeableEvent.class.getName());

    @Override
    public boolean isSkip(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
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
            logger.println(DISPLAY_NAME + ": not mergeable, skipping");
        }
        return !mergeable;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
