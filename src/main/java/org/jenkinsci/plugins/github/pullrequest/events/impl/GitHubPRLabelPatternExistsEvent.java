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

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Used to skip PR builder. Use case is skipping if PR is marked with some label or labels.
 */
public class GitHubPRLabelPatternExistsEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Labels matched to patterns";
    private final static Logger LOGGER = Logger.getLogger(GitHubPRLabelPatternExistsEvent.class.getName());

    private final GitHubPRLabel label;
    private boolean skip = true;

    @DataBoundConstructor
    public GitHubPRLabelPatternExistsEvent(GitHubPRLabel label, boolean skip) {
        this.label = label;
        this.skip = skip;
    }


    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR, @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        GitHubPRCause cause = null;
        OUT:
        for (GHLabel label : remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels()) {
            for (String labelPatternStr : this.label.getLabelsSet()) {
                Pattern labelPattern = Pattern.compile(labelPatternStr);
                if (labelPattern.matcher(label.getName()).matches()) {
                    logger.println(DISPLAY_NAME + ": Pull request has label: " + labelPatternStr);
                    LOGGER.log(Level.INFO, "Pull request has '{0}' label.", labelPatternStr);
                    cause = new GitHubPRCause(remotePR, remotePR.getUser(), "PR has label: " + labelPatternStr, isSkip(), null, null);

                    break OUT;
                }
            }
        }

        return cause;
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
