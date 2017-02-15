package com.github.kostyasha.github.integration.generic.repoprovider;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubWebHook;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import com.google.common.base.Optional;
import hudson.Extension;
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

import static com.github.kostyasha.github.integration.branch.webhook.WebhookInfoBranchPredicates.withHookTriggerMode;
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
        if (!withHookTriggerMode().apply(trigger.getJob())) return;

        GitHubWebHook.get().registerHookFor(trigger.getJob());
    }

    @Override
    public boolean isManageHooks(GitHubTrigger trigger) {
        // not exact @see https://github.com/jenkinsci/github-plugin/pull/149
        return GitHubPlugin.configuration().isManageHooks();
    }

    @Nonnull
    @Override
    public synchronized GitHub getGitHub(GitHubTrigger trigger) {
        if (isTrue(cacheConnection) && nonNull(gitHub)) {
            return gitHub;
        }

        final GitHubRepositoryName repoFullName = trigger.getRepoFullName();

        Optional<GitHub> client = from(GitHubPlugin.configuration().findGithubConfig(withHost(repoFullName.getHost())))
            .firstMatch(withAdminAccess(repoFullName));
        if (client.isPresent()) {
            gitHub = client.get();
            return gitHub;
        }

        throw new GHPluginConfigException("GitHubPluginRepoProvider can't find appropriate client for github repo " +
            "<%s>. Probably you didn't configure 'GitHub Plugin' global 'GitHub Server Settings'.or there is no tokens" +
            "with admin access to this repo.",
            repoFullName.toString());
    }

    private NullSafePredicate<GitHub> withAdminAccess(final GitHubRepositoryName name) {
        return new NullSafePredicate<GitHub>() {
            @Override
            protected boolean applyNullSafe(@Nonnull GitHub gh) {
                try {
                    return gh.getRepository(name.getUserName() + "/" + name.getRepositoryName()).hasAdminAccess();
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
