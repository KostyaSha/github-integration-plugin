package org.jenkinsci.plugins.github.pullrequest.publishers;

import hudson.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;

/**
 * Common actions for label addition and deletion.
 *
 * @author Alina Karpovich
 */
public abstract class GitHubPRAbstractPublisher extends Recorder {
    private static final Logger LOGGER = Logger.getLogger(GitHubPRAbstractPublisher.class.getName());

    private transient GHRepository ghRepository;
    private transient GHIssue ghIssue;
    private transient GHPullRequest ghPullRequest;
    private int number;
    private StatusVerifier statusVerifier;
    private PublisherErrorHandler errorHandler;

    public GitHubPRAbstractPublisher(StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        this.statusVerifier = statusVerifier;
        this.errorHandler = errorHandler;
    }

    public GHRepository getGhRepository() {
        return ghRepository;
    }

    public GHIssue getGhIssue() {
        return ghIssue;
    }

    public GHPullRequest getGhPullRequest() {
        return ghPullRequest;
    }

    public int getNumber() {
        return number;
    }

    public StatusVerifier getStatusVerifier() {
        return statusVerifier;
    }

    public PublisherErrorHandler getErrorHandler() {
        return errorHandler;
    }

    protected void handlePublisherError(AbstractBuild<?, ?> build) {
        if (errorHandler != null) {
            errorHandler.markBuildAfterError(build);
        }
    }

    public GHRepository getGhRepository(final AbstractBuild<?, ?> build) throws IOException {
        if (ghRepository == null) {
            ghRepository = build.getProject().getTrigger(GitHubPRTrigger.class).getRemoteRepo();
        }
        return ghRepository;
    }

    public int getNumber(final AbstractBuild<?, ?> build) throws AbortException {
        GitHubPRCause cause = build.getCause(GitHubPRCause.class);
        if (cause == null) {
            throw new AbortException("Can't get cause from build");
        }
        number = cause.getNumber();
        return number;
    }

    public GHIssue getGhIssue(final AbstractBuild<?, ?> build) throws IOException {
        if (ghIssue == null) {
            ghIssue = getGhRepository(build).getIssue(getNumber(build));
        }
        return ghIssue;
    }

    public GHIssue getGhPullRequest(final AbstractBuild<?, ?> build) throws IOException {
        if (ghPullRequest == null) {
            ghPullRequest = getGhRepository(build).getPullRequest(getNumber(build));
        }
        return ghPullRequest;
    }

    public static void addComment(final int id, final String comment, final AbstractBuild<?, ?> build, final TaskListener listener) {
        if (comment.trim().isEmpty()) {
            return;
        }

        String finalComment = comment;
        if (build != null && listener != null) {
            try {
                finalComment = build.getEnvironment(listener).expand(comment);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error", e);
            }
        }

        try {
            if (build != null) {
                GHRepository ghRepository = build.getProject().getTrigger(GitHubPRTrigger.class).getRemoteRepo();
                ghRepository.getPullRequest(id).comment(finalComment);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Couldn't add comment to pull request #" + id + ": '" + finalComment + "'", ex);
        }
    }

    public static GHCommitState getCommitState(final AbstractBuild<?, ?> build, final GHCommitState unstableAs) {
        GHCommitState state;
        Result result = build.getResult();
        if (result == null) {
            state = GHCommitState.ERROR;
        } else if (result.isBetterOrEqualTo(SUCCESS)) {
            state = GHCommitState.SUCCESS;
        } else if (result.isBetterOrEqualTo(UNSTABLE)) {
            state = unstableAs;
        } else {
            state = GHCommitState.FAILURE;
        }
        return state;
    }

    public final BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public final GitHubPRTrigger.DescriptorImpl getTriggerDescriptor() {
        return (GitHubPRTrigger.DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRTrigger.class);
    }

    public abstract static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public final boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public abstract String getDisplayName();
    }
}
