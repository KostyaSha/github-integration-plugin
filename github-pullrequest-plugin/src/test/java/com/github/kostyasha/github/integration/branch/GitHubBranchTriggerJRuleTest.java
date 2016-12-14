package com.github.kostyasha.github.integration.branch;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS;

/**
 * Tests that needs only JenkinsRule.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTriggerJRuleTest {
    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    /**
     * Ensure that GitHubPRRepository can be created without remote connections.
     */
    @Test
    public void repositoryInitialisationWhenProviderFails() throws Exception {
        final FreeStyleProject project = jRule.createProject(FreeStyleProject.class, "project");

        project.addProperty(new GithubProjectProperty("https://github.com/KostyaSha/test-repo/"));

        final GitHubBranchTrigger trigger = new GitHubBranchTrigger("", HEAVY_HOOKS, emptyList());
        trigger.setRepoProvider(new FailingGitHubRepoProvider());
        project.addTrigger(trigger);

        project.save();

        final FreeStyleProject freeStyleProject = (FreeStyleProject) jRule.getInstance().getItem("project");

        final GitHubBranchRepository repository = freeStyleProject.getAction(GitHubBranchRepository.class);

        assertThat(repository, notNullValue());
    }

    public static final class FailingGitHubRepoProvider extends GitHubRepoProvider {

        @DataBoundConstructor
        public FailingGitHubRepoProvider() {
        }

        @Override
        public void registerHookFor(GitHubTrigger trigger) {
            throw new GHPluginConfigException("test provider exception");
        }

        @Override
        public boolean isManageHooks(GitHubTrigger trigger) {
            return false;
        }

        @Override
        public GitHub getGitHub(GitHubTrigger trigger) {
            throw new GHPluginConfigException("test provider exception");
        }

        @Override
        public GHRepository getGHRepository(GitHubTrigger trigger) {
            throw new GHPluginConfigException("test provider exception for getGHRepository");
        }

        @TestExtension
        public static final class DescriptorImpl extends GitHubRepoProviderDescriptor {
            @Override
            public String getDisplayName() {
                return "Test GitHub Repo Provider";
            }
        }
    }

}
