package org.jenkinsci.plugins.github.pullrequest.trigger;

import antlr.ANTLRException;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRCause.newGitHubPRCause;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForCauseTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCancelQueued() throws IOException, InterruptedException, ANTLRException {
        Jenkins jenkins = j.getInstance();
        MockFolder folder = j.createFolder("folder");

        FreeStyleProject project1 = folder.createProject(FreeStyleProject.class, "project1");
        project1.setDisplayName("project1 display name");

        schedule(project1, 10, "cause1_1");

        // two PR for one project in queue
        schedule(project1, 10, "cause1_2");

        //other number for project1
        schedule(project1, 12, "cause1_3");

        FreeStyleProject project2 = folder.createProject(FreeStyleProject.class, "project2");
        project2.setDisplayName("project2 displayName");
        schedule(project2, 10, "project2");

        FreeStyleProject project3 = folder.createProject(FreeStyleProject.class, "project3");
        project2.setDisplayName("project3 displayName");
        schedule(project3, 10, "cause1_3/project3");

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(5));

        GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger.setCancelQueued(true);

        JobRunnerForCause jobRunnerForCause = new JobRunnerForCause(project1, gitHubPRTrigger);

        assertThat("Should cancel project1 with number", jobRunnerForCause.cancelQueuedBuildByPrNumber(10), is(2));

        Queue.Item[] items = jenkins.getQueue().getItems();
        assertThat(items, arrayWithSize(3));
    }

    @Test
    public void testCancelQueuedWithWorkflowJob() throws IOException, InterruptedException, ANTLRException {
        Jenkins jenkins = j.getInstance();
        Queue queue = jenkins.getQueue();
        MockFolder folder = j.createFolder("folder");

        WorkflowJob project1 = folder.createProject(WorkflowJob.class, "project1");
        project1.setDisplayName("project1 display name");

        schedule(project1, 10, "cause1_1");

        // two PR for one project in queue
        schedule(project1, 10, "cause1_2");

        //other number for project1
        schedule(project1, 12, "cause1_3");

        WorkflowJob project2 = folder.createProject(WorkflowJob.class, "project2");
        project2.setDisplayName("project2 displayName");
        schedule(project2, 10, "project2");

        WorkflowJob project3 = folder.createProject(WorkflowJob.class, "project3");
        project2.setDisplayName("project3 displayName");
        schedule(project3, 10, "cause1_3/project3");

        assertThat(queue.getItems(), arrayWithSize(5));


        GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger.setCancelQueued(true);

        JobRunnerForCause jobRunnerForCause = new JobRunnerForCause(project1, gitHubPRTrigger);

        assertThat("Should cancel project1 with number", jobRunnerForCause.cancelQueuedBuildByPrNumber(10), is(2));
        assertThat("Should cancel project1 with number", jobRunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));

        schedule(project1, 10, "cause_1");
        assertThat("Should cancel project1 with number", jobRunnerForCause.cancelQueuedBuildByPrNumber(10), is(1));

        Queue.Item[] items = queue.getItems();
        assertThat(items, arrayWithSize(3));
    }

    public static QueueTaskFuture schedule(Job<?, ?> job, int number, String param) {
        ParameterizedJobMixIn jobMixIn = JobInfoHelpers.asParameterizedJobMixIn(job);
        GitHubPRCause cause = newGitHubPRCause().withNumber(number);
        ParametersAction parametersAction = new ParametersAction(
                Collections.<ParameterValue>singletonList(new StringParameterValue("value", param))
        );
        return jobMixIn.scheduleBuild2(10000, new CauseAction(cause), parametersAction);
    }
}
