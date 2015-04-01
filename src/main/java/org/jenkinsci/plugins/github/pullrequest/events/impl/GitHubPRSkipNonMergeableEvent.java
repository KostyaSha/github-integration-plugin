package org.jenkinsci.plugins.github.pullrequest.events.impl;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Event to skip PRs that can't be merged.
 *
 * @author Alina Karpovich
 */
public class GitHubPRSkipNonMergeableEvent extends GitHubPREvent {
    private final static Logger LOGGER = Logger.getLogger(GitHubPRSkipNonMergeableEvent.class.getName());

    @Override
    public boolean isSkip(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR, GitHubPRPullRequest localPR) throws IOException {
        Boolean mergeable;
        try {
            mergeable = remotePR.getMergeable();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Can't get mergeable status: {}", e.getMessage());
            mergeable = false;
        }
        mergeable = mergeable != null ? mergeable : false;
        return !mergeable;
    }
}
