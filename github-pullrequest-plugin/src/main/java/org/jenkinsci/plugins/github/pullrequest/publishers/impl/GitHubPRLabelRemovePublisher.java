package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHLabel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;

/**
 * Implements removing of labels (one or many) from GitHub.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelRemovePublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRLabelRemovePublisher.class);

    private GitHubPRLabel labelProperty;

    @DataBoundConstructor
    public GitHubPRLabelRemovePublisher(GitHubPRLabel labelProperty,
                                        StatusVerifier statusVerifier,
                                        PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
        setLabelProperty(labelProperty);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(run)) {
            return;
        }
        try {
            HashSet<String> remoteLabels = new HashSet<String>();
            for (GHLabel label : getGhIssue(run).getLabels()) { //remote labels List -> Set
                remoteLabels.add(label.getName());
            }
            remoteLabels.removeAll(getLabelProperty().getLabelsSet());
            // TODO print only really removing
            listener.getLogger().println("Removing labels: " + getLabelProperty().getLabelsSet());
            getGhIssue(run).setLabels(remoteLabels.toArray(new String[remoteLabels.size()]));
        } catch (IOException ex) {
            listener.getLogger().println("Couldn't remove label for PR #" + getNumber(run) + " " + ex);
            LOGGER.error("Couldn't remove label for PR #{}", getNumber(), ex);
            handlePublisherError(run);
        }
    }

    public GitHubPRLabel getLabelProperty() {
        return labelProperty;
    }

    public void setLabelProperty(GitHubPRLabel labelProperty) {
        this.labelProperty = labelProperty;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPRAbstractPublisher.DescriptorImpl {
        @Override
        public String getDisplayName() {
            return "GitHub PR: remove labels";
        }
    }
}
