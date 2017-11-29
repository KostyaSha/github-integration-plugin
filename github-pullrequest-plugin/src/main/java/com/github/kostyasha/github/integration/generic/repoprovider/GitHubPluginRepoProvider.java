package com.github.kostyasha.github.integration.generic.repoprovider;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubWebHook;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import com.google.common.base.Optional;
import hudson.Extension;
import org.apache.commons.lang3.BooleanUtils;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.util.misc.NullSafePredicate;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPluginRepoProvider.class);

    // possible cache connection/repo here
    protected Boolean cacheConnection = true;

    private Boolean manageHooks = true;

    private GHPermission repoPermission = GHPermission.ADMIN;

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

    public boolean isManageHooks() {
        return BooleanUtils.isTrue(manageHooks);
    }

    @DataBoundSetter
    public void setManageHooks(boolean manageHooks) {
        this.manageHooks = manageHooks;
    }

    public GHPermission getRepoPermission() {
        return repoPermission;
    }

    @DataBoundSetter
    public void setRepoPermission(GHPermission repoPermission) {
        this.repoPermission = repoPermission;
    }

    @Override
    public void registerHookFor(GitHubTrigger trigger) {
        GitHubWebHook.get().registerHookFor(trigger.getJob());
    }

    @Override
    public boolean isManageHooks(GitHubTrigger trigger) {
        // not exact @see https://github.com/jenkinsci/github-plugin/pull/149
        return isManageHooks() && GitHubPlugin.configuration().isManageHooks();
    }

    @Nonnull
    @Override
    public synchronized GitHub getGitHub(GitHubTrigger trigger) {
        if (isTrue(cacheConnection) && nonNull(gitHub)) {
            return gitHub;
        }

        final GitHubRepositoryName repoFullName = trigger.getRepoFullName();

        Optional<GitHub> client = from(GitHubPlugin.configuration().findGithubConfig(withHost(repoFullName.getHost())))
            .firstMatch(withPermission(repoFullName, getRepoPermission()));
        if (client.isPresent()) {
            gitHub = client.get();
            return gitHub;
        }

        throw new GHPluginConfigException("GitHubPluginRepoProvider can't find appropriate client for github repo " +
            "<%s>. Probably you didn't configure 'GitHub Plugin' global 'GitHub Server Settings' or there is no tokens" +
            "with %s access to this repository.",
            repoFullName.toString(), getRepoPermission());
    }

    private NullSafePredicate<GitHub> withPermission(final GitHubRepositoryName name, GHPermission permission) {
        return new NullSafePredicate<GitHub>() {
            @Override
            protected boolean applyNullSafe(@Nonnull GitHub gh) {
                try {
                    final GHRepository repo = gh.getRepository(name.getUserName() + "/" + name.getRepositoryName());
                    if (permission == GHPermission.ADMIN) {
                        return repo.hasAdminAccess();
                    } else if (permission == GHPermission.PUSH) {
                        return repo.hasPushAccess();
                    } else {
                        return repo.hasPullAccess();
                    }
                } catch (IOException e) {
                    return false;
                }
            }
        };
    }

    @CheckForNull
    @Override
    public synchronized GHRepository getGHRepository(GitHubTrigger trigger) {
        if (isTrue(cacheConnection) && nonNull(remoteRepository)) {
            return remoteRepository;
        }

        final GitHubRepositoryName name = trigger.getRepoFullName();
        try {
            remoteRepository = getGitHub(trigger).getRepository(name.getUserName() + "/" + name.getRepositoryName());
        } catch (IOException ex) {
            LOG.error("Shouldn't fail because getGitHub() expected to provide working repo.", ex);
        }

        return remoteRepository;
    }

    protected Object readResolve() {
        if (isNull(cacheConnection)) cacheConnection = true;
        if (isNull(repoPermission)) repoPermission = GHPermission.ADMIN;
        if (isNull(manageHooks)) manageHooks = true;
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
