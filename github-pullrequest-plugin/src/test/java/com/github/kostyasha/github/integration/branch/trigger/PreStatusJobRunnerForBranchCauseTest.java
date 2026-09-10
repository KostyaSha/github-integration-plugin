package com.github.kostyasha.github.integration.branch.trigger;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchPollingLogAction;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.model.FreeStyleProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Branch counterpart of
 * {@code org.jenkinsci.plugins.github.pullrequest.trigger.PreStatusJobRunnerForCauseTest}: the
 * PENDING pre-status may only be published when a run was really scheduled, otherwise nothing ever
 * overwrites it on the commit.
 *
 * @see JobRunnerForBranchCause#apply(GitHubBranchCause)
 */
public class PreStatusJobRunnerForBranchCauseTest {
    private static final String COMMIT_SHA = "6dcb09b5b57875f334f61aebed695e2e4193db5e";
    private static final String BRANCH = "master";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private GHRepository remoteRepo;

    @Before
    public void setUp() {
        remoteRepo = mock(GHRepository.class);
    }

    @Test
    public void shouldNotPublishPendingStatusWhenJobDidNotStart() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("disabled-project");
        job.addProperty(new GithubProjectProperty("https://github.com/org/repo"));
        // makes isBuildable() false, so ParameterizedJobMixIn.scheduleBuild2() returns null
        job.disable();

        GitHubBranchTrigger trigger = preStatusTrigger(job);

        assertThat("cause shouldn't be reported as triggered",
                new JobRunnerForBranchCause(job, trigger).apply(cause()), is(false));

        verifyNoInteractions(remoteRepo);
        assertThat("nothing should be queued", j.jenkins.getQueue().getItems().length, is(0));
    }

    /**
     * Guards the assertion above: the very same setup does publish PENDING once a run is scheduled.
     */
    @Test
    public void shouldPublishPendingStatusWhenJobStarted() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("enabled-project");
        job.addProperty(new GithubProjectProperty("https://github.com/org/repo"));
        // keep the run in the queue so it can't finish and publish a final status under us
        job.setQuietPeriod(1000);

        GitHubBranchTrigger trigger = preStatusTrigger(job);

        try {
            assertThat("cause should be reported as triggered",
                    new JobRunnerForBranchCause(job, trigger).apply(cause()), is(true));

            verify(remoteRepo).createCommitStatus(eq(COMMIT_SHA), eq(GHCommitState.PENDING),
                    any(), anyString(), eq(job.getFullName()));
        } finally {
            j.jenkins.getQueue().clear();
        }
    }

    private GitHubBranchTrigger preStatusTrigger(FreeStyleProject job) throws Exception {
        GitHubBranchPollingLogAction logAction = new GitHubBranchPollingLogAction(job);
        // apply() reads the polling log into the cause, an absent file would make it return false
        // for the wrong reason (IOException)
        writeStringToFile(logAction.getPollingLogFile(), "", UTF_8);

        GitHubBranchTrigger trigger = mock(GitHubBranchTrigger.class);
        when(trigger.getPollingLogAction()).thenReturn(logAction);
        when(trigger.isPreStatus()).thenReturn(true);
        when(trigger.getRemoteRepository()).thenReturn(remoteRepo);
        return trigger;
    }

    private static GitHubBranchCause cause() throws Exception {
        GitHubBranchCause cause = new GitHubBranchCause(BRANCH, COMMIT_SHA);
        // GitHubBranchBadgeAction dereferences it while scheduling
        cause.withHtmlUrl(new URL("https://github.com/org/repo/tree/" + BRANCH));
        cause.withReason("test");
        return cause;
    }
}
