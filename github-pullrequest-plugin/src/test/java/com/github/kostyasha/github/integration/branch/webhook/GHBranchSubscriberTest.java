package com.github.kostyasha.github.integration.branch.webhook;

import antlr.ANTLRException;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Set;

import static com.github.kostyasha.github.integration.branch.webhook.GHBranchSubscriber.getBranchTriggerJobs;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Kanstantsin Shautsou
 */
public class GHBranchSubscriberTest {

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void dontFailOnBadJob() throws IOException, ANTLRException {
        String goodRepo = "https://github.com/KostyaSha-auto/test-repo";

        final FreeStyleProject job1 = jRule.createProject(FreeStyleProject.class, "bad job");
        job1.addProperty(new GithubProjectProperty("http://bad.url/deep/bad/path/"));
        job1.addTrigger(new GitHubBranchTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS_CRON, emptyList()));

        Set<Job> jobs = getBranchTriggerJobs(goodRepo);
        assertThat(jobs, hasSize(0));

        final FreeStyleProject job2 = jRule.createProject(FreeStyleProject.class, "good job");
        job2.addProperty(new GithubProjectProperty(goodRepo));
        job2.addTrigger(new GitHubBranchTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS_CRON, emptyList()));

        jobs = getBranchTriggerJobs("KostyaSha-auto/test-repo");
        assertThat(jobs, hasSize(1));
        assertThat(jobs, hasItems(job2));
    }

}