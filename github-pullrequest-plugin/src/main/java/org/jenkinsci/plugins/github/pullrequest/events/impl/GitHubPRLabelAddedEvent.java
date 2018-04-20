package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
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

import static java.util.Objects.isNull;

/**
 * When label is added to pull request. Set of labels is considered added only when
 * at least one label of set was newly added (was not saved in local PR previously)
 * AND every label of set exists on remote PR now.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelAddedEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Labels added";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRLabelAddedEvent.class);

    private final GitHubPRLabel label;

    @DataBoundConstructor
    public GitHubPRLabelAddedEvent(GitHubPRLabel label) {
        this.label = label;
    }

    public GitHubPRLabel getLabel() {
        return label;
    }

    @Override
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();

        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            return null; // already closed, skip check?
        }

        if (isNull(label)) {
            LOG.error("Label is null. Bad configured event: {}", getDescriptor().getDisplayName());
            throw new IllegalStateException("Label is null. Bad configured event: " + getDescriptor().getDisplayName());
        }

        //localPR exists before, checking for changes
        if (localPR != null && localPR.getLabels().containsAll(label.getLabelsSet())) {
            return null; // label existed before exiting
        }

        GitHubPRCause cause = null;

        Collection<GHLabel> labels = remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels();
        Set<String> existingLabels = new HashSet<String>();

        for (GHLabel curLabel : labels) {
            existingLabels.add(curLabel.getName());
        }

        if (existingLabels.containsAll(label.getLabelsSet())) {
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": state has changed (" + label.getLabelsSet() + " labels were added");
            cause = prDecisionContext.newCause(label.getLabelsSet() + " labels were added", false);
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
