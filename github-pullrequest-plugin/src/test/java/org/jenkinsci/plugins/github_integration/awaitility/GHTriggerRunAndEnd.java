package org.jenkinsci.plugins.github_integration.awaitility;

import com.github.kostyasha.github.integration.generic.GitHubPollingLogAction;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import hudson.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Callable;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GHTriggerRunAndEnd implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GHTriggerRunAndEnd.class);

    private final GitHubTrigger trigger;
    private final String oldLog;
    private final long startTime;
    private Job job;

    public GHTriggerRunAndEnd(GitHubTrigger trigger) throws IOException {
        startTime = currentTimeMillis();
        this.trigger = trigger;
        job = trigger.getJob();
        final Job<?, ?> job = trigger.getJob();
        requireNonNull(job, "Job must exist in trigger, initialise trigger!");

        final GitHubPollingLogAction oldAction = job.getAction(GitHubPollingLogAction.class);
        assertThat("Job has no action!", oldAction, notNullValue());

        oldLog = oldAction.getLog();

        trigger.run();
    }

    @Override
    public Boolean call() throws Exception {
        final GitHubPollingLogAction prLogAction = job.getAction(GitHubPollingLogAction.class);
        if (nonNull(prLogAction)) {
            final String newLog = prLogAction.getLog();
            if (nonNull(newLog) && !newLog.equals(oldLog) && newLog.contains(trigger.getFinishMsg())) {
                LOG.debug("[WAIT] trigger finished, delay {} ms", currentTimeMillis() - startTime);
                return true;
            }
        }
        return false;
    }

    public static Callable<Boolean> ghTriggerRunAndEnd(GitHubTrigger trigger) throws IOException {
        return new GHTriggerRunAndEnd(trigger);
    }
}
