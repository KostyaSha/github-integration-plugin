package org.jenkinsci.plugins.github.pullrequest;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.util.BuildData;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
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
                null, false, "nice reason", "author name", "anotherMait@mail.com");
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
        return new GitHubPRTrigger(spec, GitHubPRTriggerMode.CRON, events);
    }

    private class BuildDataBuilder extends TestBuilder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
            build.addAction(new BuildData());
            return true;
        }
    }
}
