package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;

/**
 * @author Kanstantsin Shautsou
 */
public class JobHelper {
    private JobHelper() {
    }

    public static GitHubPRTrigger ghPRTriggerFromRun(Run<?, ?> run) {
        return JobInfoHelpers.triggerFrom(run.getParent(), GitHubPRTrigger.class);
    }

    public static GitHubPRTrigger ghPRTriggerFromJob(Job<?, ?> job) {
        return JobInfoHelpers.triggerFrom(job, GitHubPRTrigger.class);
    }
}
