package org.jenkinsci.plugins.github.pullrequest.dsl;

import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.LookupStrategy;
import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.RemovedViewAction;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNumber;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS_CRON;
import static org.junit.Assert.assertThat;

public class DslIntegrationTest {

    public static final String JOB_NAME_IN_DSL_SCRIPT = "gh-pull-request";
    public static final String JOB_DSL_GROOVY = "dsl/jobdsl.groovy";
    public static final String JOB_DSL_PUBLISHER_TEXT_CONTENT = "Build finished";
    public static final String JOB_DSL_BUILDER_TEXT_CONTENT = "Building...";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldCreateJobWithExtendedDsl() throws Exception {
        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.getBuildersList().add(
                new ExecuteDslScripts(
                        new ExecuteDslScripts.ScriptLocation(
                                null, null,
                                IOUtils.toString(this
                                        .getClass().getClassLoader().getResourceAsStream(JOB_DSL_GROOVY))
                        ),
                        false,
                        RemovedJobAction.DELETE,
                        RemovedViewAction.DELETE,
                        LookupStrategy.JENKINS_ROOT
                )
        );

        jenkins.buildAndAssertSuccess(job);

        assertThat(jenkins.getInstance().getJobNames(), hasItem(is(JOB_NAME_IN_DSL_SCRIPT)));

        FreeStyleProject generated = jenkins.getInstance()
                .getItemByFullName(JOB_NAME_IN_DSL_SCRIPT, FreeStyleProject.class);

        final DescribableList<Builder, Descriptor<Builder>> builders = generated.getBuildersList();

        assertThat("Should have builder", builders, hasSize(1));
        assertThat("Should add status builder", builders.get(0), instanceOf(GitHubPRStatusBuilder.class));
        assertThat("Should add message",
                ((GitHubPRStatusBuilder) builders.get(0)).getStatusMessage().getContent(),
                equalTo(JOB_DSL_BUILDER_TEXT_CONTENT));

        verifyPublishers(generated.getPublishersList());

        Collection<Trigger<?>> triggers = generated.getTriggers().values();
        assertThat("Should add trigger", triggers, hasSize(1));
        GitHubPRTrigger trigger = (GitHubPRTrigger) triggers.toArray()[0];
        assertThat("Should add trigger of GHPR class", trigger, instanceOf(GitHubPRTrigger.class));
        assertThat("Should have pre status", trigger.isPreStatus(), equalTo(true));
        assertThat("Should have cancel queued", trigger.isCancelQueued(), equalTo(true));
        assertThat("Should set mode", trigger.getTriggerMode(), equalTo(HEAVY_HOOKS_CRON));

        final List<GitHubPREvent> events = trigger.getEvents();
        assertThat("Should add events", events, hasSize(17));


        GitHubPREvent event = events.get(15);
        assertThat(event, instanceOf(GitHubPRNumber.class));
        assertThat(((GitHubPRNumber) event).isSkip(), is(false));
        assertThat(((GitHubPRNumber) event).isMatch(), is(true));


        event = events.get(16);
        assertThat(event, instanceOf(GitHubPRNumber.class));
        assertThat(((GitHubPRNumber) event).isSkip(), is(true));
        assertThat(((GitHubPRNumber) event).isMatch(), is(true));
    }

    private void verifyPublishers(DescribableList<Publisher, Descriptor<Publisher>> publishers) {
        assertThat("Should add publisher", publishers, hasSize(2));

        assertThat("Should add status publisher", publishers.get(0), instanceOf(GitHubPRBuildStatusPublisher.class));
        assertThat("Should add 2 packages",
                ((GitHubPRBuildStatusPublisher) publishers.get(0)).getStatusMsg().getContent(),
                equalTo(JOB_DSL_PUBLISHER_TEXT_CONTENT));

        assertThat("Has comment publisher", publishers.get(1), instanceOf(GitHubPRCommentPublisher.class));
        GitHubPRCommentPublisher commentPublisher = (GitHubPRCommentPublisher) publishers.get(1);

        assertThat("Comment matches", commentPublisher.getComment().getContent(), equalTo("comment"));
        assertThat("Only failed builds", commentPublisher.getStatusVerifier().getBuildStatus(), equalTo(Result.FAILURE));
        assertThat("Publish marked as failure", commentPublisher.getErrorHandler().getBuildStatus(), equalTo(Result.FAILURE));
    }
}
