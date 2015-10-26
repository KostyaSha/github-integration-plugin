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
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Sets pr status for build caused by GitHubPRCause
 */
public class GitHubPRStatusBuilder extends Builder {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRStatusBuilder.class);

    public static final GitHubPRMessage DEFAULT_MESSAGE = new GitHubPRMessage("${GITHUB_PR_COND_REF} run started");

    @CheckForNull
    private GitHubPRMessage statusMessage = DEFAULT_MESSAGE;

    /**
     * Constructor with defaults. Only for groovy UI.
     */
    @Restricted(NoExternalUse.class)
    public GitHubPRStatusBuilder() {
    }

    @DataBoundConstructor
    public GitHubPRStatusBuilder(GitHubPRMessage statusMessage) {
        if (statusMessage != null && isNotBlank(statusMessage.getContent())) {
            this.statusMessage = statusMessage;
        }
    }

    public GitHubPRMessage getStatusMessage() {
        return statusMessage;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
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
            LOGGER.error("Can't set commit status", e);
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
            return "GitHub PR: set 'pending' status";
        }
    }
}
