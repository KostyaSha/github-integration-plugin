package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getGhIssue;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getPRNumberFromPRCause;

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
            final GHIssue ghIssue = getGhIssue(run);
            //remote labels List -> Set
            ghIssue.getLabels().stream()
                    .map(GHLabel::getName)
                    .collect(Collectors.toList())
                    .forEach(remoteLabels::add);

            remoteLabels.addAll(getLabelProperty().getLabelsSet());
            ghIssue.setLabels(remoteLabels.toArray(new String[remoteLabels.size()]));
        } catch (IOException ex) {
            final int number = getPRNumberFromPRCause(run);
            listener.getLogger().println("Couldn't add label for PR #" + number + ex.getMessage());
            LOGGER.error("Couldn't add label for PR #{}", number, ex);
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

    @Symbol("githubPRAddLabels")
    @Extension
    public static class DescriptorImpl extends GitHubPRAbstractPublisher.DescriptorImpl {
        @Override
        public String getDisplayName() {
            return "GitHub PR: add labels";
        }
    }
}
