package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Trigger run when label exists. Remove label in post-build action to exclude cycle builds.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelExistsEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Labels exist";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRLabelExistsEvent.class);

    private final GitHubPRLabel label;
    private boolean skip = true;

    @DataBoundConstructor
    public GitHubPRLabelExistsEvent(GitHubPRLabel label, boolean skip) {
        this.label = label;
        this.skip = skip;
    }

    @Override
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();

        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            return null; // already closed, skip check?
        }

        GitHubPRCause cause = null;

        Collection<GHLabel> remoteLabels = remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels();
        Set<String> existingLabels = new HashSet<>();

        for (GHLabel ghLabel : remoteLabels) {
            existingLabels.add(ghLabel.getName());
        }

        if (existingLabels.containsAll(label.getLabelsSet())) {
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": " + label.getLabelsSet() + " found");
            cause = prDecisionContext.newCause(label.getLabelsSet() + " labels exist", isSkip());
        }

        return cause;
    }

    public GitHubPRLabel getLabel() {
        return label;
    }

    public boolean isSkip() {
        return skip;
    }

    @Symbol("labelExists")
    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
