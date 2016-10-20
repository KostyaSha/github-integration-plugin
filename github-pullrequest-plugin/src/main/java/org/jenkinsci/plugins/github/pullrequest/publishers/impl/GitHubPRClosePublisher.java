package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Api;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Closes pull request after build.
 *
 * @author Alina Karpovich
 */
public class GitHubPRClosePublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRClosePublisher.class);

    @DataBoundConstructor
    public GitHubPRClosePublisher(StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(run)) {
            return;
        }

        String publishedURL = getTriggerDescriptor().getPublishedURL();
        if (publishedURL != null && !publishedURL.isEmpty()) {
            try {
                if (getGhIssue(run).getState().equals(GHIssueState.OPEN)) {
                    try {
                        getGhPullRequest(run).close();
                    } catch (IOException ex) {
                        LOGGER.error("Couldn't close the pull request #{}:", getNumber(run), ex);
                    }
                }
            } catch (IOException ex) {
                listener.getLogger().println("Can't close pull request \n" + ex);
                handlePublisherError(run);
            }
        }
    }

    public final Api getApi() {
        return new Api(this);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "GitHub PR: close PR";
        }
    }
}
