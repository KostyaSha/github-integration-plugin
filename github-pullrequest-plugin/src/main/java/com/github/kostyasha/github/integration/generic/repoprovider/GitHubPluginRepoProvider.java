package com.github.kostyasha.github.integration.generic.repoprovider;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubWebHook;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import com.google.common.base.Optional;
import hudson.Extension;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Iterator;

import static org.jenkinsci.plugins.github.config.GitHubServerConfig.withHost;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Standard github-plugin global configuration provider.
 * Defines connection based on globally configured github api servername + token.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPluginRepoProvider extends GitHubRepoProvider {
    // possible cache connection/repo here

    @DataBoundConstructor
    public GitHubPluginRepoProvider() {
    }

    @Override
    public void registerHookFor(GitHubTrigger trigger) {
        GitHubWebHook.get().registerHookFor(trigger.getJob());
    }

    @Override
    public boolean isManageHooks(GitHubTrigger trigger) {
        // not exact @see https://github.com/jenkinsci/github-plugin/pull/149
        return GitHubPlugin.configuration().isManageHooks();
    }

    @Nonnull
    @Override
    public GitHub getGitHub(GitHubTrigger trigger) {
        final GitHubRepositoryName repoFullName = trigger.getRepoFullName();

        Optional<GitHub> client = from(GitHubPlugin.configuration().findGithubConfig(withHost(repoFullName.getHost())))
                .first();
        if (client.isPresent()) {
            return client.get();
        } else {
            throw new GHPluginConfigException("Can't find appropriate client for github repo <%s>",
                    repoFullName.getHost());
        }
    }

    @CheckForNull
    @Override
    public GHRepository getGHRepository(GitHubTrigger trigger) {
        // first matched from global config
        Iterator<GHRepository> resolved = trigger.getRepoFullName().resolve().iterator();
        if (resolved.hasNext()) {
            return resolved.next();
        }

        return null;
    }

    @Extension
    public static class DescriptorImpl extends GitHubRepoProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "GitHub Plugin Repository Provider";
        }
    }
}
