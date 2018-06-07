package com.github.kostyasha.github.integration.generic;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Extension for providing GH connection for specified repository with job context.
 * You can extract additional information from job to define what connection provide.
 *
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubRepoProvider extends AbstractDescribableImpl<GitHubRepoProvider>
        implements ExtensionPoint {

    /**
     * @param trigger specific trigger type. Find by type what events wants trigger.
     */
    public abstract void registerHookFor(GitHubTrigger trigger);

    /**
     * Whether it allowed to manage hooks for certain job.
     */
    public abstract boolean isManageHooks(GitHubTrigger trigger);

    /**
     * Not used yet because trigger needs only GHRepository to work.
     */
    @CheckForNull
    public abstract GitHub getGitHub(GitHubTrigger trigger);

    /**
     * Called on trigger start. I.e. reset cache after some changes.
     */
    public void onTriggerStart() {}

    /**
     * Called on trigger stop. I.e. reset cache after some changes.
     */
    public void onTriggerStop() {}

    /**
     * alive connection to remote repo.
     */
    @CheckForNull
    public abstract GHRepository getGHRepository(GitHubTrigger trigger);


    public abstract static class GitHubRepoProviderDescriptor
            extends Descriptor<GitHubRepoProvider> {
        @Nonnull
        public abstract String getDisplayName();

        public static DescriptorExtensionList allRepoProviders() {
            return Jenkins.getInstance().getDescriptorList(GitHubRepoProvider.class);
        }
    }
}
