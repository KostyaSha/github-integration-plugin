package org.jenkinsci.plugins.github.pullrequest.trigger;

import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRCause.newGitHubPRCause;
import static org.jenkinsci.plugins.github.pullrequest.trigger.JobRunnerForCause.cancelQueuedBuildByPrNumber;
import static org.junit.Assert.*;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForCauseTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCancelQueued() throws IOException {
        Jenkins jenkins = j.getInstance();

        FreeStyleProject project1 = jenkins.createProject(FreeStyleProject.class, "project1");

        GitHubPRCause cause1 = newGitHubPRCause().withNumber(10);
        project1.scheduleBuild(1000, cause1);

        //other number for project1
        GitHubPRCause cause1_1 = newGitHubPRCause().withNumber(12);
        project1.scheduleBuild(1000, cause1_1);


        FreeStyleProject project2 = jenkins.createProject(FreeStyleProject.class, "project2");
        GitHubPRCause cause2 = newGitHubPRCause().withNumber(10);
        project2.scheduleBuild(1000, cause2);


        FreeStyleProject project3 = jenkins.createProject(FreeStyleProject.class, "project3");
        GitHubPRCause cause3 = newGitHubPRCause().withNumber(10);
        project3.scheduleBuild(1000, cause3);

        assertThat("Should cancel something", cancelQueuedBuildByPrNumber(10), is(true));

    }

}
