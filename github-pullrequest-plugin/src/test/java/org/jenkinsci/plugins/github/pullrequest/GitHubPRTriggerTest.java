package org.jenkinsci.plugins.github.pullrequest;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElementUtil;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlFormUtil;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TopLevelItem;
import hudson.plugins.git.util.BuildData;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static java.lang.String.format;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRTriggerTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public ExternalResource isWin = new ExternalResource() {
        private boolean origDefaultUseCache = true;

        @Override
        protected void before() throws Throwable {
            if (Functions.isWindows()) {
                // To avoid JENKINS-4409.
                // URLConnection caches handles to jar files by default,
                // and it prevents delete temporary directories.
                // Disable caching here.
                // Though defaultUseCache is a static field,
                // its setter and getter are provided as instance methods.
                URLConnection aConnection = new File(".").toURI().toURL().openConnection();
                origDefaultUseCache = aConnection.getDefaultUseCaches();
                aConnection.setDefaultUseCaches(false);
            }
        }

        @Override
        protected void after() {
            try {
                if (Functions.isWindows()) {
                    URLConnection aConnection = new File(".").toURI().toURL().openConnection();
                    aConnection.setDefaultUseCaches(origDefaultUseCache);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    /**
     * Snapshot of files before Branch trigger refactoring.
     */
    @LocalData
    @Test
    public void ensureOldValid() {
        final TopLevelItem item = j.getInstance().getItem("test-job");
        assertThat(item, notNullValue());
        final FreeStyleProject project = (FreeStyleProject) item;

        final GitHubPRRepository prRepository = project.getAction(GitHubPRRepository.class);
        assertThat(project, notNullValue());

        assertThat(prRepository.getFullName(), is("KostyaSha-auto/test"));

        final Map<Integer, GitHubPRPullRequest> pulls = prRepository.getPulls();
        assertThat(pulls.size(), is(1));
        final GitHubPRPullRequest pullRequest = pulls.get(1);
        assertThat(pullRequest, notNullValue());
        assertThat(pullRequest.getTitle(), is("Update README.md"));
        assertThat(pullRequest.getHeadRef(), is("KostyaSha-auto-patch-1"));
        assertThat(pullRequest.isMergeable(), is(true));
        assertThat(pullRequest.getBaseRef(), is("master"));
        assertThat(pullRequest.getUserLogin(), is("KostyaSha-auto"));
        assertThat(pullRequest.getSourceRepoOwner(), is("KostyaSha-auto"));
        assertThat(pullRequest.getLabels(), Matchers.<String>empty());

        final GitHubPRTrigger trigger = project.getTrigger(GitHubPRTrigger.class);
        assertThat(trigger, notNullValue());
        assertThat(trigger.getTriggerMode(), is(CRON));
        assertThat(trigger.getEvents(), hasSize(2));
        assertThat(trigger.isPreStatus(), is(false));
        assertThat(trigger.isCancelQueued(), is(false));
        assertThat(trigger.isSkipFirstRun(), is(false));
    }

    @Test
    public void checkBuildDataExistenceAfterBuild() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("test-job");
        p.getBuildersList().add(new BuildDataBuilder());

        FreeStyleBuild build = p.scheduleBuild2(0).get();
        j.waitUntilNoActivity();

        assertTrue(build.getActions(BuildData.class).size() > 0);
    }

    @Test
    public void checkBuildDataAbsenceAfterBuild() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject("test-job");
        p.addProperty(new GithubProjectProperty("https://github.com/KostyaSha/test-repo"));
        p.addTrigger(defaultGitHubPRTrigger());
        p.getBuildersList().add(new BuildDataBuilder());

        GitHubPRCause cause = new GitHubPRCause("headSha", 1, true, "targetBranch", "srcBranch", "mail@mail.com",
                "title", new URL("http://www.example.com"), "repoOwner", new HashSet<String>(),
                null, false, "nice reason", "author name", "anotherMait@mail.com", "open");
        FreeStyleBuild build = p.scheduleBuild2(0, cause).get();
        j.waitUntilNoActivity();

        assertTrue(build.getActions(BuildData.class).size() == 0);
    }

    @Test
    public void shouldParseRepoNameFromProp() throws IOException, ANTLRException {
        FreeStyleProject p = j.createFreeStyleProject();
        String org = "org";
        String repo = "repo";
        p.addProperty(new GithubProjectProperty(format("https://github.com/%s/%s", org, repo)));

        GitHubRepositoryName fullName = defaultGitHubPRTrigger().getRepoFullName(p);
        assertThat(fullName.getUserName(), equalTo(org));
        assertThat(fullName.getRepositoryName(), equalTo(repo));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExcOnEmptyGithubPropInGetRepoNameMethod() throws IOException, ANTLRException {
        FreeStyleProject p = j.createFreeStyleProject();
        defaultGitHubPRTrigger().getRepoFullName(p);
    }

    private static GitHubPRTrigger defaultGitHubPRTrigger() throws ANTLRException {
        String spec = "";
        List<GitHubPREvent> events = Collections.emptyList();
        return new GitHubPRTrigger(spec, CRON, events);
    }

    private class BuildDataBuilder extends TestBuilder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            build.addAction(new BuildData());
            return true;
        }
    }

    @LocalData
    @Test
    public void buildButtonsPerms() throws Exception {
        j.getInstance().setNumExecutors(0);

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy auth = new ProjectMatrixAuthorizationStrategy();
        auth.add(Jenkins.READ, "alice");
        auth.add(Computer.BUILD, "alice");

        auth.add(Jenkins.ADMINISTER, "admin");

        auth.add(Jenkins.READ, "bob");
        auth.add(Computer.BUILD, "bob");

        j.jenkins.setAuthorizationStrategy(auth);

        final FreeStyleProject project =  (FreeStyleProject) j.getInstance().getItem("project");

        Map<Permission,Set<String>> perms = new HashMap<>();

        HashSet<String> users = new HashSet<>();
        users.add("alice");
        users.add("bob");

        perms.put(Item.READ, users);

        perms.put(Item.BUILD, Collections.singleton("bob"));

        project.addProperty(new AuthorizationMatrixProperty(perms));


        JenkinsRule.WebClient webClient = j.createWebClient();
        webClient = webClient.login("bob", "bob");

        HtmlPage repoPage = webClient.getPage(project, "github-pullrequest");
        HtmlForm form = repoPage.getFormByName("rebuildAllFailed");
        HtmlFormUtil.getButtonByCaption(form, "Rebuild all failed builds").click();
        HtmlPage page = (HtmlPage) submit(form);

        Queue.Item[] items = j.getInstance().getQueue().getItems();
        assertThat(items, arrayWithSize(0));
    }
}
