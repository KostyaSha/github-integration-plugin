package org.jenkinsci.plugins.github.pullrequest.trigger;

import hudson.matrix.MatrixProject;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.hamcrest.collection.IsArrayWithSize;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import java.util.Collections;

import static org.hamcrest.Matchers.arrayWithSize;
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
    public void testFreestyleProject() throws Exception {
        test(FreeStyleProject.class);
    }

    @Test
    public void testWorkflowJob() throws Exception {
        test(WorkflowJob.class);
    }

    @Test
    public void testMatrixProject() throws Exception {
        test(MatrixProject.class);
    }

    private  <T extends TopLevelItem> void test(Class<T> tClass) throws Exception {
        Jenkins jenkins = j.getInstance();

        GlobalMatrixAuthorizationStrategy matrixAuth = new GlobalMatrixAuthorizationStrategy();
        matrixAuth.add(Jenkins.ADMINISTER, "User");
        jenkins.setAuthorizationStrategy(matrixAuth);

        MockFolder folder = j.createFolder("folder");

        Job job1 = (Job) folder.createProject(tClass, "project1");
        job1.setDisplayName("project1 display name");

        configRoundTripUnsecure(job1);

        job1.save();

        final GitHubPRTrigger gitHubPRTrigger1 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger1.setCancelQueued(true);
        gitHubPRTrigger1.start(job1, true); // to have working polling log files
        final JobRunnerForCause job1RunnerForCause = new JobRunnerForCause(job1, gitHubPRTrigger1);

        schedule(job1, 10, "cause1_1");

        // two PR for one project in queue
        schedule(job1, 10, "cause1_2");

        //other number for project1
        schedule(job1, 12, "cause1_3");

        Job job2 = (Job) folder.createProject(tClass, "project2");
        job2.setDisplayName("project2 displayName");
        job2.save();
        configRoundTripUnsecure(job2);
        schedule(job2, 10, "job2");

        final GitHubPRTrigger gitHubPRTrigger2 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger2.setCancelQueued(true);
        gitHubPRTrigger2.start(job2, true); // to have working polling log files
        final JobRunnerForCause job2RunnerForCause = new JobRunnerForCause(job2, gitHubPRTrigger2);


        Job job3 = (Job) folder.createProject(tClass, "project3");
        job3.setDisplayName("project3 displayName");
        job3.save();
        configRoundTripUnsecure(job3);
        schedule(job3, 10, "cause1_3/project3");

        final GitHubPRTrigger gitHubPRTrigger3 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger3.setCancelQueued(true);
        gitHubPRTrigger3.start(job3, true); // to have working polling log files
        final JobRunnerForCause job3RunnerForCause = new JobRunnerForCause(job3, gitHubPRTrigger3);


        Thread.sleep(1000);
        assertThat(jenkins.getQueue().getItems(), arrayWithSize(5));


        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job1 -> number 10", job1RunnerForCause.cancelQueuedBuildByPrNumber(10), is(2));
                assertThat("Should not cancel more job1 -> number 10", job1RunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(3));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job2 -> number 10", job2RunnerForCause.cancelQueuedBuildByPrNumber(10), is(1));
                assertThat("Should not cancel more job2 -> number 10", job2RunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(2));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job3 -> number 10", job3RunnerForCause.cancelQueuedBuildByPrNumber(10), is(1));
                assertThat("Should not cancel more job3 -> number 10", job3RunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(1));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job1 -> number 12", job1RunnerForCause.cancelQueuedBuildByPrNumber(12), is(1));
                assertThat("Should not cancel more job1 -> number 12", job1RunnerForCause.cancelQueuedBuildByPrNumber(12), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), IsArrayWithSize.<Queue.Item>emptyArray());
    }

    public static QueueTaskFuture schedule(Job<?, ?> job, int number, String param) {
        ParameterizedJobMixIn jobMixIn = JobInfoHelpers.asParameterizedJobMixIn(job);
        GitHubPRCause cause = newGitHubPRCause().withNumber(number);
        ParametersAction parametersAction = new ParametersAction(
                Collections.<ParameterValue>singletonList(new StringParameterValue("value", param))
        );
        return jobMixIn.scheduleBuild2(10000, new CauseAction(cause), parametersAction);
    }

    public void configRoundTripUnsecure(Job job) throws Exception {
        final AuthorizationStrategy before = j.getInstance().getAuthorizationStrategy();

        j.jenkins.setAuthorizationStrategy(new AuthorizationStrategy.Unsecured());

//        j.configRoundtrip(job);

        j.getInstance().setAuthorizationStrategy(before);
    }
}
