package org.jenkinsci.plugins.github.pullrequest.trigger;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPollingLogAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRCause.newGitHubPRCause;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The PENDING pre-status may only be published when a run was really scheduled. Without a run
 * nothing ever overwrites it and, because GitHub keeps only the newest status per context, the
 * commit stays pinned to "pending" forever.
 *
 * @see JobRunnerForCause#apply(GitHubPRCause)
 */
public class PreStatusJobRunnerForCauseTest extends JobRunnerForCauseTest {
    private static final String HEAD_SHA = "6dcb09b5b57875f334f61aebed695e2e4193db5e";

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

        GitHubPRTrigger trigger = preStatusTrigger(job);

        assertThat("cause shouldn't be reported as triggered",
                new JobRunnerForCause(job, trigger).apply(cause()), is(false));

        verifyNoInteractions(remoteRepo);
        assertThat("nothing should be queued", j.jenkins.getQueue().getItems().length, is(0));
    }

    /**
     * Guards the assertion above: the very same setup does publish PENDING once a run is scheduled,
     * so the test can't pass just because the status call was broken.
     */
    @Test
    public void shouldPublishPendingStatusWhenJobStarted() throws Exception {
        FreeStyleProject job = j.createFreeStyleProject("enabled-project");
        job.addProperty(new GithubProjectProperty("https://github.com/org/repo"));
        // keep the run in the queue so it can't finish and publish a final status under us
        job.setQuietPeriod(1000);

        GitHubPRTrigger trigger = preStatusTrigger(job);

        try {
            assertThat("cause should be reported as triggered",
                    new JobRunnerForCause(job, trigger).apply(cause()), is(true));

            verify(remoteRepo).createCommitStatus(eq(HEAD_SHA), eq(GHCommitState.PENDING),
                    anyString(), anyString(), eq(job.getFullName()));
        } finally {
            j.jenkins.getQueue().clear();
        }
    }

    private GitHubPRTrigger preStatusTrigger(FreeStyleProject job) throws Exception {
        GitHubPRPollingLogAction logAction = new GitHubPRPollingLogAction(job);
        // apply() reads the polling log into the cause, an absent file would make it return false
        // for the wrong reason (IOException)
        writeStringToFile(logAction.getPollingLogFile(), "", UTF_8);

        GitHubPRTrigger trigger = mock(GitHubPRTrigger.class);
        when(trigger.getPollingLogAction()).thenReturn(logAction);
        when(trigger.isPreStatus()).thenReturn(true);
        when(trigger.getRemoteRepository()).thenReturn(remoteRepo);
        return trigger;
    }

    private static GitHubPRCause cause() throws Exception {
        GitHubPRCause cause = newGitHubPRCause()
                .withNumber(10)
                .withHeadSha(HEAD_SHA);
        // GitHubPREnv.URL dereferences it while filling build parameters
        cause.withHtmlUrl(new URL("https://github.com/org/repo/pull/10"));
        cause.withReason("test");
        return cause;
    }
}
