package com.github.kostyasha.github.integration.branch.its;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.GitHubBranch;
import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.github_integration.junit.GHRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github_integration.awaitility.GHTriggerRunAndEnd.ghTriggerRunAndEnd;

/**
 * @author Kanstantsin Shautsou
 */
public class BranchITest {

    public static JenkinsRule jRule = new JenkinsRule();

    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static GHRule ghRule = new GHRule(jRule, temporaryFolder);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder)
            .around(jRule)
            .around(ghRule);


    public void smokeTest(Job<?, ?> job) throws Exception {
        GitHubBranchTrigger trigger;
        // fails with workflow
        if (job instanceof FreeStyleProject || job instanceof MatrixProject) {
            jRule.configRoundtrip(job); // activate trigger
            trigger = ghBranchTriggerFromJob(job);
        } else {
            trigger = ghBranchTriggerFromJob(job);
            trigger.start(job, true);
        }

        await().pollInterval(3, TimeUnit.SECONDS)
                .timeout(100, SECONDS)
                .until(ghTriggerRunAndEnd(trigger));

        jRule.waitUntilNoActivity();

        assertThat(job.getBuilds(), hasSize(3));

        GitHubBranchRepository ghRepository = job.getAction(GitHubBranchRepository.class);
        assertThat("Action storage should be available", ghRepository, notNullValue());

        Map<String, GitHubBranch> branches = ghRepository.getBranches();

        assertThat("Action storage should not to be empty", branches.entrySet(), Matchers.hasSize(3));


        ghRule.commitFileToBranch("branch-4", "someFile", "content", "With this magessage");
        await().pollInterval(3, TimeUnit.SECONDS)
                .timeout(100, SECONDS)
                .until(ghTriggerRunAndEnd(trigger));

        jRule.waitUntilNoActivity();

        // refresh objects
        ghRepository = job.getAction(GitHubBranchRepository.class);
        assertThat("Action storage should be available", ghRepository, notNullValue());

        branches = ghRepository.getBranches();
        assertThat(branches.entrySet(), Matchers.hasSize(4));

        assertThat(job.getBuilds().size(), is(4));

        jRule.assertBuildStatusSuccess(job.getLastBuild());
    }

}
