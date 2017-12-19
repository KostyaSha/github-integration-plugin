package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * When label is removed from GitHub issue(== pull request). Set of labels is considered removed only when
 * at least one label of set was newly removed (was saved in local PR previously)
 * AND every label of set was already removed from remote PR.
 *
 * @author Kanstantsin Shautsou
 * @author Alina Karpovich
 */
public class GitHubPRLabelRemovedEvent extends GitHubPREvent {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRLabelRemovedEvent.class);

    private static final String DISPLAY_NAME = "Labels removed";

    private final GitHubPRLabel label;

    @DataBoundConstructor
    public GitHubPRLabelRemovedEvent(GitHubPRLabel label) {
        this.label = label;
    }

    @Override
    public GitHubPRCause check(@Nonnull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();

        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            return null; // already closed, skip check?
        }

        if (isNull(localPR)) {  //localPR not existed before, nothing to check
            return null;
        }

        boolean hasLocal = false;
        for (String l : localPR.getLabels()) {
            for (String checkedLabel : label.getLabelsSet()) {
                if (checkedLabel.equals(l)) {
                    hasLocal = true;
                    break;
                }
            }
            if (hasLocal) {
                break;
            }
        }

        GitHubPRCause cause = null;
        boolean hasRemote = false;
        Collection<GHLabel> labels = remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels();
        for (GHLabel remoteLabel : labels) {
            for (String checkedLabel : label.getLabelsSet()) {
                if (checkedLabel.equals(remoteLabel.getName())) {
                    hasRemote = true;
                    break;
                }
            }
            if (hasRemote) {
                break;
            }
        }
        if (hasLocal && !hasRemote) { // really removed
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": state has changed ("
                    + label.getLabelsSet() + " labels were removed)");
            cause = new GitHubPRCause(remotePR, label.getLabelsSet() + " labels were removed", false);
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
            return DISPLAY_NAME;
        }
    }
}
