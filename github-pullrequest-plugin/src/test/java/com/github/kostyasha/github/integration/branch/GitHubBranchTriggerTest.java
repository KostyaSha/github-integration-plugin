package com.github.kostyasha.github.integration.branch;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlFormUtil;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCreatedEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchHashChangedEvent;
import com.github.kostyasha.github.integration.branch.test.GHMockRule;
import com.github.kostyasha.github.integration.branch.test.InjectJenkinsMembersRule;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.arrayWithSize;
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
        assertThat(lastBuild, notNullValue());

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

    @LocalData
    @Test
    public void buildButtonsPerms() throws Exception {
        jRule.getInstance().setNumExecutors(0);

        jRule.jenkins.setSecurityRealm(jRule.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy auth = new ProjectMatrixAuthorizationStrategy();
        auth.add(Jenkins.READ, "alice");
        auth.add(Computer.BUILD, "alice");

        auth.add(Jenkins.ADMINISTER, "admin");

        auth.add(Jenkins.READ, "bob");
        auth.add(Computer.BUILD, "bob");

        jRule.jenkins.setAuthorizationStrategy(auth);

        final FreeStyleProject project =  (FreeStyleProject) jRule.getInstance().getItem("project");

        Map<Permission,Set<String>> perms = new HashMap<>();

        HashSet<String> users = new HashSet<>();
        users.add("alice");
        users.add("bob");

        perms.put(Item.READ, users);

        perms.put(Item.BUILD, Collections.singleton("bob"));

        project.addProperty(new AuthorizationMatrixProperty(perms));


        JenkinsRule.WebClient webClient = jRule.createWebClient();
        webClient = webClient.login("bob", "bob");

        HtmlPage repoPage = webClient.getPage(project, "github-branch");
        HtmlForm form = repoPage.getFormByName("rebuildAllFailed");
        HtmlFormUtil.getButtonByCaption(form, "Rebuild all failed builds").click();
        HtmlPage page = (HtmlPage) submit(form);

        Queue.Item[] items = jRule.getInstance().getQueue().getItems();
        assertThat(items, arrayWithSize(0));

    }

    @TestExtension
    public static final GHMockRule.FixedGHRepoNameTestContributor CONTRIBUTOR = new GHMockRule.FixedGHRepoNameTestContributor();
}
