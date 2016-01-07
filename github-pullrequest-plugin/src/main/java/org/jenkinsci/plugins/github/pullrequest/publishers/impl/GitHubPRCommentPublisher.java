package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Api;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Adds specified text to comments after build.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommentPublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRCommentPublisher.class);

    private GitHubPRMessage comment = new GitHubPRMessage("Build ${BUILD_NUMBER} ${BUILD_RESULT}");

    /**
     * Constructor with defaults. Only for groovy UI.
     */
    @Restricted(NoExternalUse.class)
    public GitHubPRCommentPublisher() {
        super(null, null);
    }

    @DataBoundConstructor
    public GitHubPRCommentPublisher(GitHubPRMessage comment,
                                    StatusVerifier statusVerifier,
                                    PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
        this.comment = comment;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(run)) {
            return;
        }

        //TODO is this check of Jenkins public url necessary?
        //String publishedURL = getTriggerDescriptor().getPublishedURL();
        //if (publishedURL != null && !publishedURL.isEmpty()) {

        //TODO are replaceMacros, makebuildMessage needed?
        String message = comment.expandAll(run, listener);

        // Only post the build's custom message if it has been set.
        if (message != null && !message.isEmpty()) {
            try {
                getGhPullRequest(run).comment(message);
            } catch (IOException ex) {
                LOGGER.error("Couldn't add comment to pull request #{}: '{}'", getNumber(run), message, ex);
                handlePublisherError(run);
            }
            listener.getLogger().println(message);
        }
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
