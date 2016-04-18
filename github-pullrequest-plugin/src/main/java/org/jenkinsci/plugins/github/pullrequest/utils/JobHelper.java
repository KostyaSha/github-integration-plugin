package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;

import javax.annotation.CheckForNull;

/**
 * @author Kanstantsin Shautsou
 */
public class JobHelper {
    private JobHelper() {
    }

    public static GitHubPRTrigger ghPRTriggerFromRun(Run<?, ?> run) {
        return JobInfoHelpers.triggerFrom(run.getParent(), GitHubPRTrigger.class);
    }

    @CheckForNull
    public static GitHubPRTrigger ghPRTriggerFromJob(Job<?, ?> job) {
        return JobInfoHelpers.triggerFrom(job, GitHubPRTrigger.class);
    }
}
