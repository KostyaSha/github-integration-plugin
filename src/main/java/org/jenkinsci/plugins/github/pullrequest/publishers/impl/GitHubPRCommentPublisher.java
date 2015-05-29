package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Api;
import hudson.model.BuildListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds specified text to comments after build.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommentPublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRCommentPublisher.class);

    private GitHubPRMessage comment;

    @DataBoundConstructor
    public GitHubPRCommentPublisher(GitHubPRMessage comment, StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
        this.comment = comment;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(build)) {
            return true;
        }

        //TODO is this check of Jenkins public url necessary?
        //String publishedURL = getTriggerDescriptor().getPublishedURL();
        //if (publishedURL != null && !publishedURL.isEmpty()) {

        //TODO are replaceMacros, makebuildMessage needed?
        String message = comment.expandAll(build, listener);

        // Only post the build's custom message if it has been set.
        if (message != null && !message.isEmpty()) {
            try {
                getGhPullRequest(build).comment(message);
            } catch (IOException ex) {
                LOGGER.error("Couldn't add comment to pull request #{}: '{}'", getNumber(build), message, ex);
                handlePublisherError(build);
            }
            listener.getLogger().println(message);
        }
        return true;
    }

    public final Api getApi() {
        return new Api(this);
    }

    public GitHubPRMessage getComment() {
        return comment;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRCommentPublisher.class);
    }

    @Extension
    public static class DescriptorImpl extends GitHubPRAbstractPublisher.DescriptorImpl {
        @Override
        public String getDisplayName() {
            return "GitHub PR: post comment";
        }
    }
}
