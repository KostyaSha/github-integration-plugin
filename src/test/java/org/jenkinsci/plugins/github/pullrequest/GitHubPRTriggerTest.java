package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.util.BuildData;
import org.junit.*;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRTriggerTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private boolean origDefaultUseCache = true;

    @Before
    public void setUp() throws Exception {
        if(Functions.isWindows()) {
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
        p.addTrigger(new GitHubPRTrigger("", GitHubPRTriggerMode.CRON, null));
        p.getBuildersList().add(new BuildDataBuilder());

        GitHubPRCause cause = new GitHubPRCause("headSha", 1, true, "targetBranch", "srcBranch","mail@mail.com",
                "title", new URL("http://www.example.com"), "repoOwner", new HashSet<String>(),
                null, "nice reason", "author name", "anotherMait@mail.com");
        FreeStyleBuild build = p.scheduleBuild2(0, cause).get();
        j.waitUntilNoActivity();

        assertTrue(build.getActions(BuildData.class).size() == 0);
    }

    @After
    public void tearDown() throws Exception {
        if(Functions.isWindows()) {
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            aConnection.setDefaultUseCaches(origDefaultUseCache);
        }
    }

    private class BuildDataBuilder extends TestBuilder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            build.addAction(new BuildData());
            return true;
        }
    }
}
