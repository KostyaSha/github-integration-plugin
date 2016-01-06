package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
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
 * Implements addition of labels (one or many) to GitHub.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelAddPublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRLabelAddPublisher.class);

    private GitHubPRLabel labelProperty;

    @DataBoundConstructor
    public GitHubPRLabelAddPublisher(GitHubPRLabel labelProperty,
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
            HashSet<String> remoteLabels = new HashSet<>();
            for (GHLabel label : getGhIssue(run).getLabels()) { //remote labels List -> Set
                remoteLabels.add(label.getName());
            }
            remoteLabels.addAll(getLabelProperty().getLabelsSet());
            getGhIssue(run).setLabels(remoteLabels.toArray(new String[remoteLabels.size()]));
        } catch (IOException ex) {
            listener.getLogger().println("Couldn't add label for PR #" + getNumber(run) + " " + ex.getMessage());
            LOGGER.error("Couldn't add label for PR #{}", getNumber(), ex);
            handlePublisherError(run);
        }
    }

    public GitHubPRLabel getLabelProperty() {
        return labelProperty;
    }

    public void setLabelProperty(GitHubPRLabel labelProperty) {
        this.labelProperty = labelProperty;
    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRLabelAddPublisher.class);
    }

    @Extension
    public static class DescriptorImpl extends GitHubPRAbstractPublisher.DescriptorImpl {
        @Override
        public String getDisplayName() {
            return "GitHub PR: add labels";
        }
    }
}
