package org.jenkinsci.plugins.github_integration.its;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Ignore;
import org.junit.Test;

import static org.jenkinsci.plugins.github.pullrequest.util.TestUtil.classpath;


/**
 * @author Kanstantsin Shautsou
 */
public class WorkflowITest extends AbstractPRTest {

    @Test
    public void workflowTest() throws Exception {
        final WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, JOB_NAME);
        workflowJob.addTrigger(getPreconfiguredTrigger());
        workflowJob.addProperty(getPreconfiguredProperty());
        workflowJob.setQuietPeriod(10);

        workflowJob.setDefinition(
                new CpsFlowDefinition(classpath(this.getClass(), "workflow-definition.groovy"))
        );
        workflowJob.save();

//        j.pause();

        super.basicTest();
//        j.pause();
    }
}
