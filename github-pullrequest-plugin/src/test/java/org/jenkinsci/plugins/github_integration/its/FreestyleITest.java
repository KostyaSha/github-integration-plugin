package org.jenkinsci.plugins.github_integration.its;

import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;
import org.junit.Ignore;
import org.junit.Test;

import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredProperty;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredPRTrigger;

/**
 * @author Kanstantsin Shautsou
 */
@Ignore
public class FreestyleITest extends AbstractPRTest {

    @Test
    public void freestyleTest() throws Exception {
        // create job
        FreeStyleProject job = jRule.createFreeStyleProject("freestyle-job");

        job.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        job.addTrigger(getPreconfiguredPRTrigger());

        job.getBuildersList().add(new GitHubPRStatusBuilder());
        job.getBuildersList().add(new Shell("sleep 10"));

        job.getPublishersList().add(new GitHubPRBuildStatusPublisher());
        job.getPublishersList().add(new GitHubPRCommentPublisher(new GitHubPRMessage("Comment"), null, null));

        job.save();

        super.basicTest(job);
    }
}
