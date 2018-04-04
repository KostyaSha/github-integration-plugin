package org.jenkinsci.plugins.github_integration.its;

import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.util.RingBufferLogHandler;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github_integration.junit.GHRule;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHPullRequest;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.jenkinsci.plugins.github_integration.awaitility.GHPRAppeared.ghPRAppeared;
import static org.jenkinsci.plugins.github_integration.awaitility.GHTriggerRunAndEnd.ghTriggerRunAndEnd;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.BRANCH1;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractPRTest {

    public JenkinsRule jRule = new JenkinsRule();

    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public GHRule ghRule = new GHRule(jRule, temporaryFolder);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder)
        .around(jRule)
        .around(ghRule);

    public void basicTest(Job job) throws Exception {
        // fails with workflow
        if (job instanceof FreeStyleProject || job instanceof MatrixProject) {
            jRule.configRoundtrip(job); // activate trigger
        }

        // update trigger (maybe useless)
        GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
        if (job instanceof WorkflowJob) {
            trigger.start(job, true); // hack configRountrip that doesn't work with workflow
        }

        await().pollInterval(3, TimeUnit.SECONDS)
                .timeout(100, SECONDS)
                .until(ghTriggerRunAndEnd(trigger));

        jRule.waitUntilNoActivity();

        assertThat(job.getLastBuild(), is(nullValue()));

        GitHubPRRepository ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, notNullValue());

        Map<Integer, GitHubPRPullRequest> pulls = ghPRRepository.getPulls();
        assertThat("Action storage should be empty", pulls.entrySet(), Matchers.hasSize(0));

        final GHPullRequest pullRequest1 = ghRule.getGhRepo().createPullRequest("title with \"quote\" text",
                "branch-1", "master", "body");

        await().pollInterval(2, SECONDS)
                .timeout(100, SECONDS)
                .until(ghPRAppeared(pullRequest1));

        await().pollInterval(3, TimeUnit.SECONDS)
                .timeout(100, SECONDS)
                .until(ghTriggerRunAndEnd(trigger));

        jRule.waitUntilNoActivity();

        // refresh objects
        ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, notNullValue());

        pulls = ghPRRepository.getPulls();
        assertThat("Pull request 1 should appear in action storage", pulls.entrySet(), Matchers.hasSize(1));

        jRule.assertBuildStatusSuccess(job.getLastBuild());
        assertThat(job.getBuilds().size(), is(1));

        // now push changes that should trigger again
        ghRule.commitFileToBranch(BRANCH1, BRANCH1 + ".file2", "content", "commit 2 for " + BRANCH1);

        trigger.run();

        await().pollInterval(5, TimeUnit.SECONDS)
                .timeout(100, SECONDS)
                .until(ghTriggerRunAndEnd(trigger));

        jRule.waitUntilNoActivity();

        // refresh objects
        ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, notNullValue());

        pulls = ghPRRepository.getPulls();
        assertThat("Pull request 1 should appear in action storage", pulls.entrySet(), Matchers.hasSize(1));

        jRule.assertBuildStatusSuccess(job.getLastBuild());
        assertThat(job.getBuilds().size(), is(2));
    }

}
