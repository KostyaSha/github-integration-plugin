package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import com.google.common.annotations.Beta;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import jenkins.util.NonLocalizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.github.kostyasha.github.integration.multibranch.category.GitHubBranchSCMHeadCategory.BRANCH;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubPRSCMHeadCategory.PR;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubTagSCMHeadCategory.TAG;
import static java.util.Arrays.asList;


public class GitHubSCMSource extends SCMSource {
    /**
     * This will the URL to the project main branch.
     * One for PRs and branches... etc...
     */
    private String projectUrlStr;

    // one for tags, etc
    @Beta
    private List<GitHubRepoProvider> repoProviders = asList(new GitHubPluginRepoProvider()); // default
    private transient GitHubRepoProvider repoProvider = null;

    private GitHubBranchHandler branchHandler;
    private GitHubPRHandler prHandler;



    @DataBoundConstructor
    public GitHubSCMSource() {
    }


    @DataBoundSetter
    public void setProjectUrlStr(String projectUrlStr) {
        this.projectUrlStr = projectUrlStr;
    }

    public String getProjectUrlStr() {
        return projectUrlStr;
    }


    public List<GitHubRepoProvider> getRepoProviders() {
        return repoProviders;
    }

    @DataBoundSetter
    public void setRepoProviders(List<GitHubRepoProvider> repoProviders) {
        this.repoProviders = repoProviders;
    }


    @Override
    protected void retrieve(SCMSourceCriteria scmSourceCriteria,
                            @Nonnull SCMHeadObserver scmHeadObserver, SCMHeadEvent<?> scmHeadEvent,
                            @Nonnull TaskListener taskListener) throws IOException, InterruptedException {

        taskListener.getLogger().println("test retrieve");

        GitHubBranchSCMHead branchHead = new GitHubBranchSCMHead("someBranch");

        scmHeadObserver.observe(branchHead, new SCMRevisionImpl(branchHead, UUID.randomUUID().toString()));


        GitHubTagSCMHead taggy = new GitHubTagSCMHead("taggy");
        scmHeadObserver.observe(taggy, new SCMRevisionImpl(taggy, UUID.randomUUID().toString()));

    }

    @Nonnull
    @Override
    public SCM build(@Nonnull SCMHead scmHead, SCMRevision scmRevision) {

//        return new GitSCM("repo url");
        return new NullSCM();
    }

    @Override
    protected boolean isCategoryEnabled(@Nonnull SCMHeadCategory category) {
        return true;
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceDescriptor {

        @Nonnull
        @Override
        protected SCMHeadCategory[] createCategories() {
            // array? bundled in descriptor??  seriously?
            return new SCMHeadCategory[]{BRANCH, PR, TAG, new UncategorizedSCMHeadCategory(new NonLocalizable("Lost&Found"))};
        }


        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub source";
        }
    }
}
