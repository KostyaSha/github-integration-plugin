package org.jenkinsci.plugins.github.pullrequest;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.git.util.BuildData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Sets Pending build status before build run and manipulates Git's BuildData attached to job Action.
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRBuildListener extends RunListener<AbstractBuild<?, ?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRBuildListener.class);

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, @Nonnull TaskListener listener) {
        GitHubPRTrigger trigger = build.getProject().getTrigger(GitHubPRTrigger.class);
        if (trigger == null) {
            return;
        }

        GitHubPRCause cause = build.getCause(GitHubPRCause.class);

        if (cause != null) {
            //remove all BuildData, because it doesn't work right with pull requests now
            //TODO rework after git-client patching about BuildData usage
            build.getActions().removeAll(build.getActions(BuildData.class));
        }
    }

    @Override
    public void onStarted(AbstractBuild<?, ?> build, TaskListener listener) {
        GitHubPRTrigger trigger = build.getProject().getTrigger(GitHubPRTrigger.class);
        if (trigger == null) {
            return;
        }

        GitHubPRCause c = build.getCause(GitHubPRCause.class);
        if (c == null) {
            return;
        }

        // short build description shown in history
        try {
            build.setDescription("<a title=\"" + c.getTitle() + "\" href=\"" + c.getHtmlUrl() + "\">PR #"
                    + c.getNumber() + "</a>: " + c.getAbbreviatedTitle());
        } catch (IOException e) {
            LOGGER.error("Can't set build description", e);
        }
    }
}
