package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.branch.test.GHMockRule;
import com.github.kostyasha.github.integration.branch.test.InjectJenkinsMembersRule;
import com.github.kostyasha.github.integration.generic.repoprovider.GHPermission;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import org.hamcrest.Matchers;
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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
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


    /**
     * loading old local state data, running trigger and checking that old disappeared and new appeared
     */
    @LocalData
    @Test
    public void actualiseRepo() throws Exception {
        config.getConfigs().add(github.serverConfig());
        config.save();

        Thread.sleep(1000);

        FreeStyleProject project = (FreeStyleProject) jRule.getInstance().getItem("project");
        assertThat(project, notNullValue());

        GitHubPRTrigger trigger = project.getTrigger(GitHubPRTrigger.class);
        assertThat(trigger, notNullValue());

        GitHubRepositoryName repoFullName = trigger.getRepoFullName();
        assertThat(repoFullName.getHost(), is("localhost"));
        assertThat(repoFullName.getUserName(), is("org"));
        assertThat(repoFullName.getRepositoryName(), is("old-repo"));

        GitHubPRRepository prRepository = project.getAction(GitHubPRRepository.class);
        assertThat(project, notNullValue());

        assertThat(prRepository.getFullName(), is("org/old-repo"));
        assertThat(prRepository.getGithubUrl(), notNullValue());
        assertThat(prRepository.getGithubUrl().toString(), is("https://localhost/org/old-repo/"));
        assertThat(prRepository.getGitUrl(), is("git://localhost/org/old-repo.git"));
        assertThat(prRepository.getSshUrl(), is("git@localhost:org/old-repo.git"));

        final Map<Integer, GitHubPRPullRequest> pulls = prRepository.getPulls();
        assertThat(pulls.size(), is(1));

        final GitHubPRPullRequest pullRequest = pulls.get(1);
        assertThat(pullRequest, notNullValue());
        assertThat(pullRequest.getNumber(), is(1));
        assertThat(pullRequest.getHeadSha(), is("65d0f7818009811e5d5eb703ebad38bbcc816b49"));
        assertThat(pullRequest.isMergeable(), is(true));
        assertThat(pullRequest.getBaseRef(), is("master"));
        assertThat(pullRequest.getHtmlUrl().toString(), is("https://localhost/org/old-repo/pull/1"));

        // now new
        project.addProperty(new GithubProjectProperty("http://localhost/org/repo"));

        GitHubPluginRepoProvider repoProvider = new GitHubPluginRepoProvider();
        repoProvider.setManageHooks(false);
        repoProvider.setRepoPermission(GHPermission.PULL);

        trigger.setRepoProvider(repoProvider);
        project.addTrigger(trigger);
        project.save();

        // activate trigger
        jRule.configRoundtrip(project);

        trigger = project.getTrigger(GitHubPRTrigger.class);

        trigger.run();

        jRule.waitUntilNoActivity();

        assertThat(project.getBuilds(), hasSize(1));

        repoFullName = trigger.getRepoFullName();
        assertThat(repoFullName.getHost(), Matchers.is("localhost"));
        assertThat(repoFullName.getUserName(), Matchers.is("org"));
        assertThat(repoFullName.getRepositoryName(), Matchers.is("repo"));


        GitHubPRPollingLogAction logAction = project.getAction(GitHubPRPollingLogAction.class);
        assertThat(logAction, notNullValue());
        assertThat(logAction.getLog(),
                containsString("Repository full name changed from 'org/old-repo' to 'org/repo'.\n"));

        assertThat(logAction.getLog(),
                containsString("Changing GitHub url from 'https://localhost/org/old-repo/' " +
                        "to 'http://localhost/org/repo'.\n"));
        assertThat(logAction.getLog(),
                containsString("Changing Git url from 'git://localhost/org/old-repo.git' " +
                        "to 'git://localhost/org/repo.git'.\n"));
        assertThat(logAction.getLog(),
                containsString("Changing SSH url from 'git@localhost:org/old-repo.git' " +
                        "to 'git@localhost:org/repo.git'.\n"));
        assertThat(logAction.getLog(),
                containsString("Local settings changed, removing PRs in repository!"));

    }
}
