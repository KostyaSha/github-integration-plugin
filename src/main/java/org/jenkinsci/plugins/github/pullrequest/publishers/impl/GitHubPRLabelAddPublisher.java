package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHLabel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements addition of labels (one or many) to GitHub.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelAddPublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = Logger.getLogger(GitHubPRLabelAddPublisher.class.getName());

    private GitHubPRLabel labelProperty;

    @DataBoundConstructor
    public GitHubPRLabelAddPublisher(GitHubPRLabel labelProperty, StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
        setLabelProperty(labelProperty);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(build)) {
            return true;
        }
        try {
            HashSet<String> remoteLabels = new HashSet<String>();
            for (GHLabel label : getGhIssue(build).getLabels()) { //remote labels List -> Set
                remoteLabels.add(label.getName());
            }
            remoteLabels.addAll(getLabelProperty().getLabelsSet());
            getGhIssue(build).setLabels(remoteLabels.toArray(new String[remoteLabels.size()]));
        } catch (IOException ex) {
            listener.getLogger().println("Couldn't add label for PR #" + getNumber(build) + " " + ex.getMessage());
            LOGGER.log(Level.SEVERE, "Couldn't add label for PR #" + getNumber(), ex);
            handlePublisherError(build);
        }
        return true;
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
