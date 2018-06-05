package org.jenkinsci.plugins.github.pullrequest;

import com.github.kostyasha.github.integration.branch.test.GHMockRule;
import com.github.kostyasha.github.integration.branch.test.InjectJenkinsMembersRule;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRTriggerMockTest {
    @Inject
    public GitHubPluginConfig config;

    public JenkinsRule jRule = new JenkinsRule();

    @Rule
    public RuleChain chain = RuleChain.outerRule(jRule)
            .around(new InjectJenkinsMembersRule(jRule, this));

    @Rule
    public GHMockRule github = new GHMockRule(
            new WireMockRule(
                    wireMockConfig().dynamicPort().notifier(new Slf4jNotifier(true))
            ))
            .stubRateLimit()
            .stubPulls()
            .stubComments1()
            .stubIssues1()
            .stubUser()
            .stubRepo();

    @TestExtension
    public static final GHMockRule.FixedGHRepoNameTestContributor CONTRIBUTOR = new GHMockRule.FixedGHRepoNameTestContributor();


    @LocalData
    @Test
    public void badStatePR() throws InterruptedException, IOException {
        config.getConfigs().add(github.serverConfig());
        config.save();

        Thread.sleep(1000);

        final TopLevelItem item = jRule.getInstance().getItem("test-job");
        assertThat(item, notNullValue());
        final FreeStyleProject project = (FreeStyleProject) item;

        GitHubPRRepository prRepository = project.getAction(GitHubPRRepository.class);
        assertThat(project, notNullValue());

        assertThat(prRepository.getFullName(), is("org/repo"));

        final Map<Integer, GitHubPRPullRequest> pulls = prRepository.getPulls();
        assertThat(pulls.size(), is(1));
        final GitHubPRPullRequest pullRequest = pulls.get(1);
        assertThat(pullRequest, notNullValue());
        assertThat(pullRequest.isInBadState(), is(true));


        final GitHubPRTrigger trigger = project.getTrigger(GitHubPRTrigger.class);
        assertThat(trigger, notNullValue());

        trigger.run();

        GitHubPRPollingLogAction logAction = project.getAction(GitHubPRPollingLogAction.class);
        assertThat(logAction, notNullValue());

        assertThat(logAction.getLog(), containsString("ERROR: local PR [#1 new-feature] is in bad state"));

        trigger.run();

        logAction = project.getAction(GitHubPRPollingLogAction.class);

        assertThat(logAction.getLog(), not(containsString("ERROR: local PR [#1 new-feature] is in bad state")));
        assertThat(logAction.getLog(), containsString("PR [#1 new-feature] not changed"));
    }



    @LocalData
    @Test
    public void actualiseRepo() throws InterruptedException {
        config.getConfigs().add(github.serverConfig());
        config.save();

        Thread.sleep(1000);

        FreeStyleProject project = (FreeStyleProject) jRule.getInstance().getItem("project");

    }
}
