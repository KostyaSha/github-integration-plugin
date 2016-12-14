package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.branch.GitHubBranchTriggerJRuleTest;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRTriggerJRuleTest {

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    /**
     * Ensure that GitHubPRRepository can be created without remote connections.
     */
    @Test
    public void repositoryInitialisationWhenProviderFails() throws Exception {
        final FreeStyleProject project = jRule.createProject(FreeStyleProject.class, "project");

        project.addProperty(new GithubProjectProperty("https://github.com/KostyaSha/test-repo/"));

        final GitHubPRTrigger prTrigger = new GitHubPRTrigger("", HEAVY_HOOKS, emptyList());
        prTrigger.setRepoProvider(new GitHubBranchTriggerJRuleTest.FailingGitHubRepoProvider());
        project.addTrigger(prTrigger);

        project.save();

        final FreeStyleProject freeStyleProject = (FreeStyleProject) jRule.getInstance().getItem("project");

        final GitHubPRRepository repository = freeStyleProject.getAction(GitHubPRRepository.class);

        assertThat(repository, notNullValue());
    }
}
