package org.jenkinsci.plugins.github.pullrequest.webhook;

import antlr.ANTLRException;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Set;

import static com.cloudbees.jenkins.GitHubRepositoryName.create;
import static com.google.common.base.Charsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.webhook.GHPullRequestSubscriber.getPRTriggerJobs;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class GHPullRequestSubscriberTest {

    public static final String REPO_URL_FROM_PAYLOAD = "https://github.com/lanwen/test";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private GitHubPRTrigger trigger;

    @Test
    public void dontFailOnBadJob() throws IOException, ANTLRException {
        String goodRepo = "https://github.com/KostyaSha-auto/test-repo";

        final FreeStyleProject job1 = jenkins.createProject(FreeStyleProject.class, "bad job");
        job1.addProperty(new GithubProjectProperty("http://bad.url/deep/bad/path/"));
        job1.addTrigger(new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS_CRON, emptyList()));

        Set<Job> jobs = getPRTriggerJobs(goodRepo);
        MatcherAssert.assertThat(jobs, hasSize(0));

        final FreeStyleProject job2 = jenkins.createProject(FreeStyleProject.class, "good job");
        job2.addProperty(new GithubProjectProperty(goodRepo));
        job2.addTrigger(new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS_CRON, emptyList()));

        jobs = getPRTriggerJobs("KostyaSha-auto/test-repo");
        MatcherAssert.assertThat(jobs, hasSize(1));
        MatcherAssert.assertThat(jobs, hasItems(job2));
    }

    @Test
    public void shouldTriggerJobOnPullRequestOpen() throws Exception {
        when(trigger.getRepoFullName(any(AbstractProject.class))).thenReturn(create(REPO_URL_FROM_PAYLOAD));
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(new GHSubscriberEvent(
                "",
                GHEvent.PULL_REQUEST,
                classpath("payload/pull_request.json"))
        );

        verify(trigger).queueRun(eq(job), eq(1));
    }

    @Test
    public void shouldTriggerFreestyleProjectOnIssueComment() throws Exception {
        when(trigger.getRepoFullName(any(Job.class))).thenReturn(create(REPO_URL_FROM_PAYLOAD));
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(new GHSubscriberEvent(
                "",
                GHEvent.ISSUE_COMMENT,
                classpath("payload/issue_comment.json"))
        );

        verify(trigger).queueRun(eq(job), eq(1));
    }

    @Test
    public void shouldTriggerWorkflowJobOnIssueComment() throws Exception {
        when(trigger.getRepoFullName(any(Job.class))).thenReturn(create(REPO_URL_FROM_PAYLOAD));
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        WorkflowJob job = jenkins.getInstance().createProject(WorkflowJob.class, "workflow-job");
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(new GHSubscriberEvent(
                "",
                GHEvent.ISSUE_COMMENT,
                classpath("payload/issue_comment.json"))
        );

        verify(trigger).queueRun(eq(job), eq(1));
    }

    @Test
    public void shouldNotBeApplicableWithoutTrigger() throws Exception {
        FreeStyleProject job = jenkins.createFreeStyleProject();
        assertThat("only for jobs with trigger", new GHPullRequestSubscriber().isApplicable(job), is(false));
    }

    @Test
    public void shouldNotBeApplicableWithCronTrigger() throws Exception {
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.CRON);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addTrigger(trigger);

        assertThat("should ignore cron trigger", new GHPullRequestSubscriber().isApplicable(job), is(false));
    }

    @Test
    public void shouldBeApplicableWithHeavyHooksTrigger() throws Exception {
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addTrigger(trigger);

        assertThat("only for jobs with trigger with hook", new GHPullRequestSubscriber().isApplicable(job), is(true));
    }

    @Test
    public void shouldBeApplicableWithCronHooksTrigger() throws Exception {
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS_CRON);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addTrigger(trigger);

        assertThat("only for jobs with trigger with hook", new GHPullRequestSubscriber().isApplicable(job), is(true));
    }

    @Test
    public void shouldBeApplicableWithLightHooksTrigger() throws Exception {
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.LIGHT_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addTrigger(trigger);

        assertThat("only for jobs with trigger with hook", new GHPullRequestSubscriber().isApplicable(job), is(true));
    }

    public static String classpath(String path) throws IOException {
        return IOUtils.toString(GHPullRequestSubscriberTest.class.getClassLoader().getResourceAsStream(path), UTF_8);
    }
}
