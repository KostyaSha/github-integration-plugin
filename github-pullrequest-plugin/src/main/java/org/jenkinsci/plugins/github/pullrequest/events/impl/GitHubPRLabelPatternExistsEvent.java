package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Pattern;

/**
 * Used to skip PR builder. Use case is skipping if PR is marked with some label or labels.
 */
public class GitHubPRLabelPatternExistsEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Labels matched to patterns";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRLabelPatternExistsEvent.class);

    private final GitHubPRLabel label;
    private boolean skip = true;

    @DataBoundConstructor
    public GitHubPRLabelPatternExistsEvent(GitHubPRLabel label, boolean skip) {
        this.label = label;
        this.skip = skip;
    }


    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        for (GHLabel ghLabel : remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels()) {
            for (String labelPatternStr : label.getLabelsSet()) {
                Pattern labelPattern = Pattern.compile(labelPatternStr);
                if (labelPattern.matcher(ghLabel.getName()).matches()) {
                    logger.println(DISPLAY_NAME + ": Pull request has label: " + labelPatternStr);
                    LOGGER.info("Pull request has '{}' label.", labelPatternStr);
                    return new GitHubPRCause(remotePR, "PR has label: " + labelPatternStr, isSkip());
                }
            }
        }

        return null;
    }

    public GitHubPRLabel getLabel() {
        return label;
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
