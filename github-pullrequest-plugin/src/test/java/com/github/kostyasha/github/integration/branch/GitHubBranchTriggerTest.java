package com.github.kostyasha.github.integration.branch;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubPushTrigger;
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
import hudson.model.Result;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import javax.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.junit.Assert.*;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.when;

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
            .stubRepoBranches()
            .stubRepo()
            .stubStatuses();

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

        branchTrigger.run();

        jRule.waitUntilNoActivity();

        assertThat(prj.getBuilds(), hasSize(1));
        final FreeStyleBuild lastBuild = prj.getLastBuild();

        final GitHubBranchCause cause = lastBuild.getCause(GitHubBranchCause.class);
        assertThat(cause, notNullValue());
        assertThat(cause.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193db5e"));
        assertThat(cause.getBranchName(), is("master"));
    }

    @TestExtension
    public static final GHMockRule.FixedGHRepoNameTestContributor CONTRIBUTOR = new GHMockRule.FixedGHRepoNameTestContributor();
}