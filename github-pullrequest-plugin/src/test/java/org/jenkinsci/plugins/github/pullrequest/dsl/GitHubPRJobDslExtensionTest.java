package org.jenkinsci.plugins.github.pullrequest.dsl;

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchRestrictionFilter;
import com.github.kostyasha.github.integration.branch.events.impl.commitchecks.impl.GitHubBranchCommitMessageCheck;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import hudson.model.FreeStyleProject;
import hudson.triggers.Trigger;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.LookupStrategy;
import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.RemovedViewAction;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collection;
import java.util.List;

import static com.github.kostyasha.github.integration.generic.repoprovider.GHPermission.PULL;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS_CRON;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRJobDslExtensionTest {

    public static final String JOB_NAME_IN_DSL_SCRIPT = "gh-branch";
    public static final String JOB_DSL_GROOVY = "dsl/branch-jobdsl.groovy";

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


        Collection<Trigger<?>> triggers = generated.getTriggers().values();
        assertThat("Should add trigger", triggers, hasSize(1));
        GitHubBranchTrigger trigger = (GitHubBranchTrigger) triggers.toArray()[0];
        assertThat("Should add trigger of GHPR class", trigger, instanceOf(GitHubBranchTrigger.class));
        assertThat("Should have pre status", trigger.isPreStatus(), equalTo(true));
        assertThat("Should have cancel queued", trigger.isCancelQueued(), equalTo(true));
        assertThat("Should add events", trigger.getEvents(), hasSize(5));
        assertThat("Should set mode", trigger.getTriggerMode(), equalTo(HEAVY_HOOKS_CRON));

        /*
         * note: this are explicity placed in the first and second positions so it's easier to
         * pluck them back out for explicit testing
         */
        verifyBranchFilter(trigger.getEvents().get(0));
        verifyCommitChecks(trigger.getEvents().get(1));

        final List<GitHubRepoProvider> repoProviders = trigger.getRepoProviders();
        assertThat("Should contain repoProvider", repoProviders, notNullValue());
        assertThat("Should contain 1 repoProvider", repoProviders, hasSize(1));

        final GitHubRepoProvider repoProvider = repoProviders.get(0);
        assertThat(repoProvider, instanceOf(GitHubPluginRepoProvider.class));
        final GitHubPluginRepoProvider provider = (GitHubPluginRepoProvider) repoProvider;
        assertThat(provider.isCacheConnection(), is(false));
        assertThat(provider.isManageHooks(), is(false));
        assertThat(provider.getRepoPermission(), is(PULL));

    }

    private void verifyCommitChecks(GitHubBranchEvent gitHubBranchEvent) {
        GitHubBranchCommitEvent commitChecks = (GitHubBranchCommitEvent) gitHubBranchEvent;
        assertThat("Has commit checks", commitChecks.getChecks(), hasSize(1));

        GitHubBranchCommitMessageCheck messageCheck = (GitHubBranchCommitMessageCheck) commitChecks.getChecks().get(0);
        assertThat("Has match critiera", messageCheck.getMatchCriteria(), notNullValue());
    }

    private void verifyBranchFilter(GitHubBranchEvent gitHubBranchEvent) {
        GitHubBranchRestrictionFilter filter = (GitHubBranchRestrictionFilter) gitHubBranchEvent;

        assertThat("Has branches", filter.getMatchCriteria(), hasSize(2));
        assertThat("Branches match", filter.getMatchCriteria(), containsInAnyOrder("master", "other"));
    }
}
