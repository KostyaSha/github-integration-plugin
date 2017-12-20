package com.github.kostyasha.github.integration.branch;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCreatedEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchHashChangedEvent;
import com.github.kostyasha.github.integration.branch.test.GHMockRule;
import com.github.kostyasha.github.integration.branch.test.InjectJenkinsMembersRule;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTriggerTest {
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
            .stubUser()
            .stubRepoBranchShouldChange()
            .stubRepoBranches()
            .stubRepo()
            .stubStatuses();

    @LocalData
    @Test
    public void someTest() throws Exception {
        config.getConfigs().add(github.serverConfig());
        config.save();

        Thread.sleep(1000);

        FreeStyleProject prj = jRule.createFreeStyleProject("project");
        prj.addProperty(new GithubProjectProperty("http://localhost/org/repo"));

        final List<GitHubBranchEvent> events = new ArrayList<>();
        events.add(new GitHubBranchCreatedEvent());
        events.add(new GitHubBranchHashChangedEvent());

        final GitHubBranchTrigger trigger = new GitHubBranchTrigger("", CRON, events);
        prj.addTrigger(trigger);
        prj.save();
        // activate trigger
        jRule.configRoundtrip(prj);

        final GitHubBranchTrigger branchTrigger = prj.getTrigger(GitHubBranchTrigger.class);

        assertThat(branchTrigger.getRemoteRepository(), notNullValue());

        GitHubBranchRepository localRepo = prj.getAction(GitHubBranchRepository.class);
        assertThat(localRepo, notNullValue());
        assertThat(localRepo.getBranches().size(), is(2));
        assertThat(localRepo.getBranches(), hasKey("for-removal"));
        assertThat(localRepo.getBranches(), hasKey("should-change"));
        GitHubBranch shouldChange = localRepo.getBranches().get("should-change");
        assertThat(shouldChange.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193ffbb"));

        // only single branch should change in local repo
        branchTrigger.doRun("should-change");
        jRule.waitUntilNoActivity();

        assertThat(prj.getBuilds(), hasSize(1));
        FreeStyleBuild lastBuild = prj.getLastBuild();

        GitHubBranchCause cause = lastBuild.getCause(GitHubBranchCause.class);
        assertThat(cause, notNullValue());
        assertThat(cause.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193ffgg"));
        assertThat(cause.getBranchName(), is("should-change"));

        localRepo = prj.getAction(GitHubBranchRepository.class);
        assertThat(localRepo, notNullValue());
        assertThat(localRepo.getBranches().size(), is(2));
        assertThat(localRepo.getBranches(), hasKey("for-removal"));
        assertThat(localRepo.getBranches(), hasKey("should-change"));
        shouldChange = localRepo.getBranches().get("should-change");
        assertThat(shouldChange.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193ffgg"));


        // and now full trigger run()
        branchTrigger.run();

        jRule.waitUntilNoActivity();

        assertThat(prj.getBuilds(), hasSize(2));
        lastBuild = prj.getLastBuild();

        cause = lastBuild.getCause(GitHubBranchCause.class);
        assertThat(cause, notNullValue());
        assertThat(cause.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193db5e"));
        assertThat(cause.getBranchName(), is("new-branch"));

        localRepo = prj.getAction(GitHubBranchRepository.class);
        assertThat(localRepo.getBranches().size(), is(2));
        assertThat(localRepo.getBranches(), not(hasKey("for-removal")));
        assertThat(localRepo.getBranches(), hasKey("should-change"));

        shouldChange = localRepo.getBranches().get("should-change");
        assertThat(shouldChange.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193ffgg"));

        assertThat(localRepo.getBranches(), hasKey("new-branch"));
        GitHubBranch branch = localRepo.getBranches().get("new-branch");
        assertThat(branch.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193db5e"));

    }

    @TestExtension
    public static final GHMockRule.FixedGHRepoNameTestContributor CONTRIBUTOR = new GHMockRule.FixedGHRepoNameTestContributor();
}