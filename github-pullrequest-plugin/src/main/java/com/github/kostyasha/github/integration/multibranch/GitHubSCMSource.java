package com.github.kostyasha.github.integration.multibranch;

import com.cloudbees.hudson.plugins.folder.computed.ComputedFolder;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import com.github.kostyasha.github.integration.multibranch.action.GitHubSCMSourcesReposAction;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import com.google.common.annotations.Beta;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMProbe;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceDescriptor;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.impl.UncategorizedSCMHeadCategory;
import jenkins.util.NonLocalizable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.kostyasha.github.integration.multibranch.category.GitHubBranchSCMHeadCategory.BRANCH;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubPRSCMHeadCategory.PR;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubTagSCMHeadCategory.TAG;
import static java.util.Arrays.asList;


public class GitHubSCMSource extends SCMSource {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubSCMSource.class);
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


    @CheckForNull
    public GitHubBranchHandler getBranchHandler() {
        return branchHandler;
    }

    @DataBoundSetter
    public GitHubSCMSource setBranchHandler(GitHubBranchHandler branchHandler) {
        this.branchHandler = branchHandler;
        return this;
    }

    @CheckForNull
    public GitHubPRHandler getPrHandler() {
        return prHandler;
    }

    @DataBoundSetter
    public GitHubSCMSource setPrHandler(GitHubPRHandler prHandler) {
        this.prHandler = prHandler;
        return this;
    }


    protected getStorageAction() {
        ComputedFolder owner = (ComputedFolder) getOwner();

        GitHubSCMSourcesReposAction sourcesReposAction = owner.getAction(GitHubSCMSourcesReposAction.class);
        sourcesReposAction.get
    }

    @Override
    protected void retrieve(SCMSourceCriteria scmSourceCriteria,
                            @Nonnull SCMHeadObserver scmHeadObserver, SCMHeadEvent<?> scmHeadEvent,
                            @Nonnull TaskListener taskListener) throws IOException, InterruptedException {

        taskListener.getLogger().println("> GitHubSCMSource.retrieve(jenkins.scm.api.SCMSourceCriteria, jenkins.scm.api.SCMHeadObserver, jenkins.scm.api.SCMHeadEvent<?>, hudson.model.TaskListener)");

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


    // It is so hard to implement good APIs...
    @Nonnull
    @Override
    public Set<SCMRevision> parentRevisions(@Nonnull SCMHead head,
                                            @Nonnull SCMRevision revision,
                                            @CheckForNull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.parentRevisions");
        return super.parentRevisions(head, revision, listener);
    }

    @Nonnull
    @Override
    public Map<SCMHead, SCMRevision> parentHeads(@Nonnull SCMHead head,
                                                 @CheckForNull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> .GitHubSCMSource.parentHeads(SCMHead, TaskListener");
        return super.parentHeads(head, listener);
    }

    @Nonnull
    @Override
    protected Set<SCMHead> retrieve(@Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> Retrieve Set<SCMHead> = GitHubSCMSource.retrieve(hudson.model.TaskListener)");
        return super.retrieve(listener);
    }

    @Nonnull
    @Override
    protected Set<SCMHead> retrieve(@CheckForNull SCMSourceCriteria criteria,
                                    @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> Retrieve Set<SCMHead> GitHubSCMSource.retrieve(jenkins.scm.api.SCMSourceCriteria, hudson.model.TaskListener)");
        listener.getLogger().println(">> criteria " + criteria);
        return super.retrieve(criteria, listener);
    }

    @Override
    protected SCMRevision retrieve(@Nonnull SCMHead head,
                                   @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> Retrieve Set<SCMHead> GitHubSCMSource.retrieve(jenkins.scm.api.SCMHead, hudson.model.TaskListener)");
        listener.getLogger().println(">> head " + head);

        return super.retrieve(head, listener);
    }

    @Override
    protected SCMRevision retrieve(@Nonnull String thingName,
                                   @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> SCMRevision = GitHubSCMSource.retrieve(java.lang.String, hudson.model.TaskListener)");
        listener.getLogger().println(">> thingName " + thingName);
        return super.retrieve(thingName, listener);
    }

    @Nonnull
    @Override
    protected Set<String> retrieveRevisions(@Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveRevisions");
        return super.retrieveRevisions(listener);
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMRevision revision,
                                           @CheckForNull SCMHeadEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveActions(jenkins.scm.api.SCMRevision, jenkins.scm.api.SCMHeadEvent, hudson.model.TaskListener)");

        return super.retrieveActions(revision, event, listener);
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMHead head,
                                           @CheckForNull SCMHeadEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveActions(jenkins.scm.api.SCMHead, jenkins.scm.api.SCMHeadEvent, hudson.model.TaskListener)");
        listener.getLogger().println(">> head " + head + " event " + event);

        return super.retrieveActions(head, event, listener);
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@CheckForNull SCMSourceEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveActions(jenkins.scm.api.SCMSourceEvent, hudson.model.TaskListener)");
        listener.getLogger().println(">> sourceEvent " + event);
        return super.retrieveActions(event, listener);
    }

    @Nonnull
    @Override
    public SCMRevision getTrustedRevision(@Nonnull SCMRevision revision,
                                          @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.getTrustedRevision");
        listener.getLogger().println(">> revision " + revision);
        return super.getTrustedRevision(revision, listener);
    }

    @Override
    public boolean canProbe() {
        return super.canProbe();
    }

    @Nonnull
    @Override
    protected SCMProbe createProbe(@Nonnull SCMHead head,
                                   @CheckForNull SCMRevision revision) throws IOException {
        LOG.debug("CreateProbe");
        return super.createProbe(head, revision);
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
