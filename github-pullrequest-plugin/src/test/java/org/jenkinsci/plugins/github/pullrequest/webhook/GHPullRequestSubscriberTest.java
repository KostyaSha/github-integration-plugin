package org.jenkinsci.plugins.github.pullrequest.webhook;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.cloudbees.jenkins.GitHubRepositoryName.create;
import static com.google.common.base.Charsets.UTF_8;
import static org.hamcrest.core.Is.is;
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
    public static final String REPO_FROM_PAYLOAD = "lanwen/test";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private GitHubPRTrigger trigger;

    @Test
    public void shouldTriggerJobOnPullRequestOpen() throws Exception {
        when(trigger.getRepoFullName(any(AbstractProject.class))).thenReturn(create(REPO_URL_FROM_PAYLOAD));
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(GHEvent.PULL_REQUEST, classpath("payload/pull_request.json"));

        verify(trigger).queueRun(eq(job), eq(1));
    }

    @Test
    public void shouldTriggerJobOnIssueComment() throws Exception {
        when(trigger.getRepoFullName(any(AbstractProject.class))).thenReturn(create(REPO_FROM_PAYLOAD));
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(GHEvent.ISSUE_COMMENT, classpath("payload/issue_comment.json"));

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
