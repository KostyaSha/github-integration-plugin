package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trigger run when label exists. Remove label in post-build action to exclude cycle builds.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelExistsEvent extends GitHubPREvent {
    private static final Logger LOGGER = Logger.getLogger(GitHubPRLabelExistsEvent.class.getName());

    private final GitHubPRLabel label;

    @DataBoundConstructor
    public GitHubPRLabelExistsEvent(GitHubPRLabel label) {
        this.label = label;
    }

    @Override
    public GitHubPRCause isStateChanged(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR, @CheckForNull GitHubPRPullRequest localPR) throws IOException {
        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            return null; // already closed, skip check?
        }

        GitHubPRCause cause = null;

        Collection<GHLabel> ghLabels = remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels();
        Set<String> existingLabels = new HashSet<String>();

        for (GHLabel ghLabel : ghLabels) {
            existingLabels.add(ghLabel.getName());
        }

        if (existingLabels.containsAll(label.getLabelsSet())) {
            cause = new GitHubPRCause(remotePR, remotePR.getUser(), label.getLabelsSet() + " labels exist", null, null);
        }

        return cause;
    }

    public GitHubPRLabel getLabel() {
        return label;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return "Labels exist";
        }
    }
}
