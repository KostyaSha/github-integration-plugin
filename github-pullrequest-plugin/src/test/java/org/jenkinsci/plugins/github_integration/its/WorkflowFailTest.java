package org.jenkinsci.plugins.github_integration.its;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithTimeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kanstantsin Shautsou
 */
@Ignore(value = "Test class")
public class WorkflowFailTest {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowFailTest.class);

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @WithTimeout(value = 0)
    @Test
    public void testWFHSError() throws Exception {
        final WorkflowJob workflow = j.getInstance().createProject(WorkflowJob.class, "workflow");
        workflow.save();
        LOG.info(workflow.getAbsoluteUrl());
        j.configRoundtrip(workflow); // fails
//        j.pause();
    }
}
