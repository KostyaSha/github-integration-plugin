package org.jenkinsci.plugins.github.pullrequest;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.git.util.BuildData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromRun;

/**
 * Sets Pending build status before build run and manipulates Git's BuildData attached to job Action.
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRBuildListener extends RunListener<Run<?, ?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRBuildListener.class);

    @Override
    public void onCompleted(Run<?, ?> run, @Nonnull TaskListener listener) {
        GitHubPRTrigger trigger = ghPRTriggerFromRun(run);
        if (trigger == null) {
            return;
        }

        GitHubPRCause cause = run.getCause(GitHubPRCause.class);

        if (cause != null) {
            //remove all BuildData, because it doesn't work right with pull requests now
            //TODO rework after git-client patching about BuildData usage
            run.getActions().removeAll(run.getActions(BuildData.class));
        }
    }

    @Override
    public void onStarted(Run<?, ?> run, TaskListener listener) {
        GitHubPRTrigger trigger = ghPRTriggerFromRun(run);
        if (trigger == null) {
            return;
        }

        GitHubPRCause c = run.getCause(GitHubPRCause.class);
        if (c == null) {
            return;
        }

        // short build description shown in history
        try {
            run.setDescription("<a title=\"" + c.getTitle() + "\" href=\"" + c.getHtmlUrl() + "\">PR #"
                    + c.getNumber() + "</a>: " + c.getAbbreviatedTitle());
        } catch (IOException e) {
            LOGGER.error("Can't set build description", e);
        }
    }
}
