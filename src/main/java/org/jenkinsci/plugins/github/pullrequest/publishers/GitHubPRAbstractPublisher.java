package org.jenkinsci.plugins.github.pullrequest.publishers;

import hudson.AbortException;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromRun;

/**
 * Common actions for label addition and deletion.
 *
 * @author Alina Karpovich
 */
public abstract class GitHubPRAbstractPublisher extends Recorder implements SimpleBuildStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRAbstractPublisher.class);

    @CheckForNull
    private transient GHRepository ghRepository;
    @CheckForNull
    private transient GHIssue ghIssue;
    @CheckForNull
    private transient GHPullRequest ghPullRequest;
    @CheckForNull
    private int number;

    @CheckForNull
    private StatusVerifier statusVerifier;
    @CheckForNull
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

    protected void handlePublisherError(Run<?, ?> run) {
        if (errorHandler != null) {
            errorHandler.markBuildAfterError(run);
        }
    }

    public GHRepository getGhRepository(final Run<?, ?> run) throws IOException {
        if (ghRepository == null) {
            ghRepository = ghPRTriggerFromRun(run).getRemoteRepo();
        }
        return ghRepository;
    }

    public int getNumber(final Run<?, ?> run) throws AbortException {
        GitHubPRCause cause = run.getCause(GitHubPRCause.class);
        if (cause == null) {
            throw new AbortException("Can't get cause from build");
        }
        number = cause.getNumber();
        return number;
    }

    public GHIssue getGhIssue(final Run<?, ?> run) throws IOException {
        if (ghIssue == null) {
            ghIssue = getGhRepository(run).getIssue(getNumber(run));
        }
        return ghIssue;
    }

    public GHIssue getGhPullRequest(final Run<?, ?> build) throws IOException {
        if (ghPullRequest == null) {
            ghPullRequest = getGhRepository(build).getPullRequest(getNumber(build));
        }
        return ghPullRequest;
    }

    public static void addComment(final int id, final String comment, final Run<?, ?> run, final TaskListener listener) {
        if (comment.trim().isEmpty()) {
            return;
        }

        String finalComment = comment;
        if (run != null && listener != null) {
            try {
                finalComment = run.getEnvironment(listener).expand(comment);
            } catch (Exception e) {
                LOGGER.error("Error", e);
            }
        }

        try {
            if (run != null) {
                final GitHubPRTrigger trigger = JobInfoHelpers.triggerFrom(run.getParent(), GitHubPRTrigger.class);

                GHRepository ghRepository = trigger.getRemoteRepo();
                ghRepository.getPullRequest(id).comment(finalComment);
            }
        } catch (IOException ex) {
            LOGGER.error("Couldn't add comment to pull request #{}: '{}'", id, finalComment, ex);
        }
    }

    public static GHCommitState getCommitState(final Run<?, ?> run, final GHCommitState unstableAs) {
        GHCommitState state;
        Result result = run.getResult();
        if (result == null) {
            LOGGER.error("{} result is null.", run);
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
