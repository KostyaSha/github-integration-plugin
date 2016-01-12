package org.jenkinsci.plugins.github_integration.its;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Kanstantsin Shautsou
 */
public class FreestyleITest extends AbstractPRTest {

    @Test
    public void freestyleTest() throws Exception {
        // create job
        FreeStyleProject job = j.createFreeStyleProject(JOB_NAME);

        job.addProperty(getPreconfiguredProperty());

        job.addTrigger(getPreconfiguredTrigger());

        job.getBuildersList().add(new GitHubPRStatusBuilder());
        job.getBuildersList().add(new Shell("sleep 10"));

        job.getPublishersList().add(new GitHubPRBuildStatusPublisher());
        job.getPublishersList().add(new GitHubPRCommentPublisher(new GitHubPRMessage("Comment"), null, null));

        job.save();

        super.basicTest();
    }
}
