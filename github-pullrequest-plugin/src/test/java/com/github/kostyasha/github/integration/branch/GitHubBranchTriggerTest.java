package com.github.kostyasha.github.integration.branch;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlFormUtil;
import org.htmlunit.html.HtmlPage;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCreatedEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchHashChangedEvent;
import com.github.kostyasha.github.integration.branch.test.GHMockRule;
import com.github.kostyasha.github.integration.branch.test.InjectJenkinsMembersRule;
import com.github.kostyasha.github.integration.generic.repoprovider.GHPermission;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.awaitility.Awaitility;
import hudson.model.CauseAction;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.htmlunit.html.HtmlFormUtil.submit;
import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTriggerTest {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchTriggerTest.class);

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

    @Before
    public void before() throws InterruptedException {
        config.getConfigs().add(github.serverConfig());
        config.save();

        Thread.sleep(1000);
    }

    @LocalData
    @Test
    public void someTest() throws Exception {
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
        branchTrigger.doRun();

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

        final FreeStyleProject project = (FreeStyleProject) jRule.getInstance().getItem("project");

        Map<Permission, Set<String>> perms = new HashMap<>();

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

    @LocalData
    @Test
    public void actualiseRepo() throws Exception {
        Thread.sleep(1000);

        FreeStyleProject project = (FreeStyleProject) jRule.getInstance().getItem("project");

        GitHubBranchTrigger branchTrigger = ghBranchTriggerFromJob(project);

        GitHubRepositoryName repoFullName = branchTrigger.getRepoFullName();
        assertThat(repoFullName.getHost(), Matchers.is("github.com"));
        assertThat(repoFullName.getUserName(), Matchers.is("KostyaSha-auto"));
        assertThat(repoFullName.getRepositoryName(), Matchers.is("test"));

        GitHubBranchRepository localRepo = project.getAction(GitHubBranchRepository.class);
        assertThat(localRepo, notNullValue());

        assertThat(localRepo.getFullName(), is("KostyaSha-auto/test"));
        assertThat(localRepo.getGithubUrl(), notNullValue());
        assertThat(localRepo.getGithubUrl().toString(), is("https://github.com/KostyaSha-auto/test/"));
        assertThat(localRepo.getSshUrl(), is("git@github.com:KostyaSha-auto/test.git"));
        assertThat(localRepo.getGitUrl(), is("git://github.com/KostyaSha-auto/test.git"));

        assertThat(localRepo.getBranches().size(), is(2));
        assertThat(localRepo.getBranches(), hasKey("old-repo"));
        assertThat(localRepo.getBranches(), hasKey("old-branch"));

        GitHubBranch oldRepo = localRepo.getBranches().get("old-repo");
        assertThat(oldRepo.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193fffb"));
        assertThat(oldRepo.getHtmlUrl(), is("https://github.com/KostyaSha-auto/test/tree/old-repo"));
        assertThat(oldRepo.getName(), is("old-repo"));

        GitHubBranch oldBranch = localRepo.getBranches().get("old-branch");
        assertThat(oldBranch.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193ffbe"));
        assertThat(oldBranch.getHtmlUrl(), is("https://github.com/KostyaSha-auto/test/tree/old-branch"));
        assertThat(oldBranch.getName(), is("old-branch"));


        project.addProperty(new GithubProjectProperty("http://localhost/org/repo"));

        GitHubPluginRepoProvider repoProvider = new GitHubPluginRepoProvider();
        repoProvider.setManageHooks(false);
        repoProvider.setRepoPermission(GHPermission.PULL);

        final List<GitHubBranchEvent> events = new ArrayList<>();
        events.add(new GitHubBranchCreatedEvent());
        events.add(new GitHubBranchHashChangedEvent());

        branchTrigger = new GitHubBranchTrigger("", CRON, events);
        branchTrigger.setRepoProvider(repoProvider);
        project.addTrigger(branchTrigger);
        project.save();
        // activate trigger
        jRule.configRoundtrip(project);

        branchTrigger = project.getTrigger(GitHubBranchTrigger.class);
        // and now full trigger run()
        branchTrigger.doRun();

        jRule.waitUntilNoActivity();

        repoFullName = branchTrigger.getRepoFullName();
        assertThat(repoFullName.getHost(), Matchers.is("localhost"));
        assertThat(repoFullName.getUserName(), Matchers.is("org"));
        assertThat(repoFullName.getRepositoryName(), Matchers.is("repo"));


        GitHubBranchPollingLogAction logAction = project.getAction(GitHubBranchPollingLogAction.class);
        assertThat(logAction, Matchers.notNullValue());
        assertThat(logAction.getLog(),
                containsString("Repository full name changed from 'KostyaSha-auto/test' to 'org/repo'.\n"));

        assertThat(logAction.getLog(),
                containsString("Changing GitHub url from 'https://github.com/KostyaSha-auto/test/' " +
                        "to 'http://localhost/org/repo'.\n"));
        assertThat(logAction.getLog(),
                containsString("Changing Git url from 'git://github.com/KostyaSha-auto/test.git' " +
                        "to 'git://localhost/org/repo.git'.\n"));
        assertThat(logAction.getLog(),
                containsString("Changing SSH url from 'git@github.com:KostyaSha-auto/test.git' " +
                        "to 'git@localhost:org/repo.git'.\n"));
        assertThat(logAction.getLog(),
                containsString("Local settings changed, removing branches in repository!"));

        assertThat(project.getBuilds(), hasSize(2));

        localRepo = project.getAction(GitHubBranchRepository.class);

        // now expect actualisation
        assertThat(localRepo.getFullName(), is("org/repo"));
        assertThat(localRepo.getGithubUrl(), notNullValue());
        assertThat(localRepo.getGithubUrl().toString(), is("http://localhost/org/repo"));
        assertThat(localRepo.getGitUrl(), is("git://localhost/org/repo.git"));
        assertThat(localRepo.getSshUrl(), is("git@localhost:org/repo.git"));

        assertThat(localRepo.getBranches().size(), is(2));
        assertThat(localRepo.getBranches(), not(hasKey("old-branch")));
        assertThat(localRepo.getBranches(), not(hasKey("old-repo")));

        assertThat(localRepo.getBranches(), hasKey("should-change"));
        GitHubBranch shouldChange = localRepo.getBranches().get("should-change");
        assertThat(shouldChange.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193ffgg"));
        assertThat(shouldChange.getHtmlUrl(), is("http://localhost/org/repo/tree/should-change"));

        assertThat(localRepo.getBranches(), hasKey("new-branch"));
        GitHubBranch newBranch = localRepo.getBranches().get("new-branch");
        assertThat(newBranch.getCommitSha(), is("6dcb09b5b57875f334f61aebed695e2e4193db5e"));
        assertThat(newBranch.getHtmlUrl(), is("http://localhost/org/repo/tree/new-branch"));
    }

    @Test
    public void asyncTestQueue() throws Exception {
        Jenkins jenkins = jRule.getInstance();
        Thread.sleep(2000);

        github.service().setGlobalFixedDelay(2_000);

        FreeStyleProject project = jRule.getInstance().createProject(FreeStyleProject.class, "new-job");

        project.addProperty(new GithubProjectProperty("http://localhost/org/repo"));
        project.save();

        GitHubBranchTrigger trigger = new GitHubBranchTrigger("", GitHubPRTriggerMode.CRON,
                Arrays.asList(new GitHubBranchCreatedEvent()));

        GitHubPluginRepoProvider repoProvider = new GitHubPluginRepoProvider();
        repoProvider.setManageHooks(false);
        repoProvider.setRepoPermission(GHPermission.PULL);

        trigger.setRepoProvider(repoProvider);

        project.addTrigger(trigger);
//        project.getBuildersList().add(new SleepBuilder(20_000));
        project.save();

        // activate trigger
        jRule.configRoundtrip(project);

        trigger = ghBranchTriggerFromJob(project);

        jenkins.setNumExecutors(0);

        // now fill sequential queue
        for (int i = 1; i < 10; i++) {
//            trigger.queueRun(1);
            trigger.queueRun(null);
        }

        Awaitility.await()
                .timeout(120, TimeUnit.SECONDS)
                .until(() -> {
                    Queue.Item[] items = jenkins.getQueue().getItems();
                    LOG.info("Jenkins Queue: {}", items.length);

                    return items.length == 2;
                });

        List<Queue.Item> items = jenkins.getQueue().getItems(project);

        assertThat(items, hasSize(2));

        for (Queue.Item item : items) {
            CauseAction causeAction = item.getAction(CauseAction.class);
            assertThat(causeAction, notNullValue());

            assertThat("Async issues in doRun()", causeAction.getCauses(), not(hasSize(6)));
            assertThat(causeAction.getCauses(), hasSize(1));
        }

        jenkins.setNumExecutors(2);
        //thread break point
        jRule.waitUntilNoActivity();

        assertThat(project.getBuilds(), hasSize(2));

    }

    @TestExtension
    public static final GHMockRule.FixedGHRepoNameTestContributor CONTRIBUTOR = new GHMockRule.FixedGHRepoNameTestContributor();
}
