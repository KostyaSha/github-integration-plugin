package com.github.kostyasha.github.integration.generic.repoprovider;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubWebHook;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import com.google.common.base.Optional;
import hudson.Extension;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Iterator;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static jenkins.scm.api.SCMHeadObserver.first;
import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
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
    protected Boolean cacheConnection = true;

    private transient GHRepository remoteRepository;
    private transient GitHub gitHub;

    @DataBoundConstructor
    public GitHubPluginRepoProvider() {
    }

    public boolean isCacheConnection() {
        return isNotFalse(cacheConnection);
    }

    @DataBoundSetter
    public void setCacheConnection(boolean cacheConnection) {
        this.cacheConnection = cacheConnection;
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
        if (isTrue(cacheConnection) && nonNull(gitHub)) {
            return gitHub;
        }

        final GitHubRepositoryName repoFullName = trigger.getRepoFullName();

        final FluentIterableWrapper<GitHub> gitHubs = from(GitHubPlugin.configuration()
            .findGithubConfig(withHost(repoFullName.getHost())));

        for (GitHub gh : gitHubs) {
            try {
                if (gh.getRepository(repoFullName.getUserName() + "/" + repoFullName.getRepositoryName()).hasAdminAccess()) {
                    gitHub = gh;
                    return gitHub;
                }
            } catch (IOException ignore) {
            }

        }

        throw new GHPluginConfigException("GitHubPluginRepoProvider can't find appropriate client for github repo <%s>",
            repoFullName.getHost());
    }

    @CheckForNull
    @Override
    public GHRepository getGHRepository(GitHubTrigger trigger) {
        if (isTrue(cacheConnection) && nonNull(remoteRepository)) {
            return remoteRepository;
        }
        // first matched from global config
        Iterator<GHRepository> resolved = trigger.getRepoFullName().resolve().iterator();
        if (resolved.hasNext()) {
            remoteRepository = resolved.next();
            return remoteRepository;
        }

        return null;
    }

    protected Object readResolve() {
        if (isNull(cacheConnection)) cacheConnection = true;
        return this;
    }

    @Extension
    public static class DescriptorImpl extends GitHubRepoProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "GitHub Plugin Repository Provider";
        }
    }
}
