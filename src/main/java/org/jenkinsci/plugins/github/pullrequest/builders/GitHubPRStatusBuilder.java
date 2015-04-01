package org.jenkinsci.plugins.github.pullrequest.builders;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sets pr status for build caused by GitHubPRCause
 */
public class GitHubPRStatusBuilder extends Builder {
    private static final Logger LOGGER = java.util.logging.Logger.getLogger(GitHubPRStatusBuilder.class.getName());

    private GitHubPRMessage statusMessage = new GitHubPRMessage("$GITHUB_PR_COND_REF run started");

    public GitHubPRStatusBuilder() {
    }

    @DataBoundConstructor
    public GitHubPRStatusBuilder(GitHubPRMessage statusMessage) {
        if (statusMessage != null && statusMessage.getContent() != null && !"".equals(statusMessage.getContent())) {
            this.statusMessage = statusMessage;
        }
    }

    public GitHubPRMessage getStatusMessage() {
        return statusMessage;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        GitHubPRTrigger trigger = build.getProject().getTrigger(GitHubPRTrigger.class);
        if (trigger == null) {
            return true;
        }

        GitHubPRCause cause = build.getCause(GitHubPRCause.class);
        if (cause == null) {
            return true;
        }

        // GitHub status for commit
        try {
            if (statusMessage != null) {
                String url = trigger.getDescriptor().getJenkinsURL() + build.getUrl();

                trigger.getRemoteRepo().createCommitStatus(cause.getHeadSha(),
                        GHCommitState.PENDING,
                        url,
                        statusMessage.expandAll(build, listener),
                        build.getProject().getFullName());
            }
        } catch (IOException e) {
            listener.getLogger().println("Can't update build description");
            LOGGER.log(Level.SEVERE, "Can't set commit status", e);
        }

        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Set pull request status to \"pending\" on GitHub";
        }
    }
}
