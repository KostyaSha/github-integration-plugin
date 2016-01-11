package org.jenkinsci.plugins.github_integration.its;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class WorkflowFailTest {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowFailTest.class);

    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Test
    public void testWFHSError() throws IOException {
        final WorkflowJob workflow = j.getInstance().createProject(WorkflowJob.class, "workflow");
        LOG.info(workflow.getAbsoluteUrl());
        j.pause();
    }
}
