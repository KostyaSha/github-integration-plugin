package com.github.kostyasha.github.integration.multibranch.repoprovider;

import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import javax.annotation.Nonnull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubRepoProvider2 extends AbstractDescribableImpl<GitHubRepoProvider2> implements ExtensionPoint {


    public abstract void registerHookFor(GitHubSCMSource source);

    public abstract boolean isManageHooks(GitHubSCMSource source);

    @Nonnull
    public abstract GitHub getGitHub(GitHubSCMSource source);

    public abstract GHRepository getGHRepository(GitHubSCMSource source);

    public abstract static class GitHubRepoProviderDescriptor2
            extends Descriptor<GitHubRepoProvider2> {
        @Nonnull
        public abstract String getDisplayName();

        public static DescriptorExtensionList allRepoProviders2() {
            return Jenkins.getInstance().getDescriptorList(GitHubRepoProvider2.class);
        }
    }
}
