package org.jenkinsci.plugins.github.pullrequest.webhook;

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

import static com.google.common.base.Charsets.UTF_8;
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
        when(trigger.getRepoFullName(any(AbstractProject.class))).thenReturn(REPO_FROM_PAYLOAD);
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(GHEvent.PULL_REQUEST, classpath("payload/pull_request.json"));

        verify(trigger).queueRun(eq(job), eq(1));
    }

    @Test
    public void shouldTriggerJobOnIssueComment() throws Exception {
        when(trigger.getRepoFullName(any(AbstractProject.class))).thenReturn(REPO_FROM_PAYLOAD);
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.addProperty(new GithubProjectProperty(REPO_URL_FROM_PAYLOAD));
        job.addTrigger(trigger);

        new GHPullRequestSubscriber().onEvent(GHEvent.ISSUE_COMMENT, classpath("payload/issue_comment.json"));

        verify(trigger).queueRun(eq(job), eq(1));
    }

    public static String classpath(String path) throws IOException {
        return IOUtils.toString(GHPullRequestSubscriberTest.class.getClassLoader().getResourceAsStream(path), UTF_8);
    }
}
