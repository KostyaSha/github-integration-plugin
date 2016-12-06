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
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRCauseFromRun;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromRun;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * Common actions for label addition and deletion.
 *
 * @author Alina Karpovich
 */
public abstract class GitHubPRAbstractPublisher extends Recorder implements SimpleBuildStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRAbstractPublisher.class);

    @CheckForNull
    private StatusVerifier statusVerifier;

    @CheckForNull
    private PublisherErrorHandler errorHandler;

    public GitHubPRAbstractPublisher(StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        this.statusVerifier = statusVerifier;
        this.errorHandler = errorHandler;
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
