package com.github.kostyasha.github.integration.multibranch;

import com.cloudbees.hudson.plugins.folder.computed.ComputedFolder;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.action.GitHubSCMSourcesReposAction;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import com.github.kostyasha.github.integration.multibranch.repoprovider.GitHubRepoProvider2;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import com.google.common.base.Throwables;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
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
import jenkins.scm.api.SCMSourceOwner;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.github.kostyasha.github.integration.multibranch.category.GitHubBranchSCMHeadCategory.BRANCH;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubPRSCMHeadCategory.PR;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubTagSCMHeadCategory.TAG;
import static com.google.common.base.Preconditions.checkState;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;


public class GitHubSCMSource extends SCMSource {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubSCMSource.class);
    /**
     * This will the URL to the project main branch.
     * One for PRs and branches... etc...
     */
    private String projectUrlStr;

    // one for tags, etc
    private GitHubRepoProvider2 repoProvider = null;

    private List<GitHubHandler> handlers = new ArrayList<>();

    private transient GitHubSCMSourcesReposAction reposAction;

    @DataBoundConstructor
    public GitHubSCMSource() {
    }


    public GitHubRepositoryName getRepoFullName() {
        return GitHubRepositoryName.create(projectUrlStr);
    }

    @DataBoundSetter
    public void setProjectUrlStr(String projectUrlStr) {
        this.projectUrlStr = projectUrlStr;
    }

    public String getProjectUrlStr() {
        return projectUrlStr;
    }

    @CheckForNull
    public GitHubRepoProvider2 getRepoProvider() {
        return repoProvider;
    }

    @DataBoundSetter
    public GitHubSCMSource setRepoProvider(GitHubRepoProvider2 repoProvider) {
        this.repoProvider = repoProvider;
        return this;
    }

    public List<GitHubHandler> getHandlers() {
        return handlers;
    }

    @DataBoundSetter
    public GitHubSCMSource setHandlers(List<GitHubHandler> handlers) {
        this.handlers = handlers;
        return this;
    }

    protected GitHubRepo getLocalRepo() {
        return getReposAction().getOrCreate(this);
    }

    protected synchronized GitHubSCMSourcesReposAction getReposAction(){
        ComputedFolder owner = (ComputedFolder) getOwner();

        // TransientActions are not persisted between getAllActions() calls.
        if (isNull(reposAction)) {
            // or stop asking job for Action and work directly with folder and action
            reposAction = owner.getAction(GitHubSCMSourcesReposAction.class);
        }

        return reposAction;
    }

    @Override
    protected void retrieve(SCMSourceCriteria scmSourceCriteria,
                            @Nonnull SCMHeadObserver scmHeadObserver,
                            SCMHeadEvent<?> scmHeadEvent, // null for manual run
                            @Nonnull TaskListener taskListener) throws IOException, InterruptedException {
        PrintStream llog = taskListener.getLogger();
        llog.println("Source id " + getId());

        llog.println("> GitHubSCMSource.retrieve(jenkins.scm.api.SCMSourceCriteria, jenkins.scm.api.SCMHeadObserver, jenkins.scm.api.SCMHeadEvent<?>, hudson.model.TaskListener)");
        llog.println(">> scmSourceCriteria " + scmSourceCriteria);
        llog.println(">> scmHeadObserver " + scmHeadObserver);
        llog.println(">> scmHeadEvent " + scmHeadEvent);

        GitHubRepo localRepo = getLocalRepo();

        // TODO actualise some repo for UI Action?
        localRepo.actualize(getRemoteRepo());
        
        SCMHeadConsumer consumer = new GithubSCMHeadConsumer(this, scmHeadObserver, scmSourceCriteria, taskListener);

        getHandlers().forEach(handler -> {
            try {
                handler.handle(consumer, localRepo, getRemoteRepo(), taskListener, this);
            } catch (IOException e) {
                LOG.error("Can't process handler", e);
                e.printStackTrace(llog);
            }
        });
    }

    @Override
    public void afterSave() {
        SCMSourceOwner owner = getOwner();
        if (nonNull(owner) && getRepoProvider().isManageHooks(this)) {
            getRepoProvider().registerHookFor(this);
        }
    }

    @Nonnull
    public GHRepository getRemoteRepo() throws IOException {
        Objects.requireNonNull(repoProvider);
        GHRepository remoteRepository = repoProvider.getGHRepository(this);
        checkState(nonNull(remoteRepository), "Can't get remote GH repo for source %s", getId());
        return remoteRepository;
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
        GitHubSCMRevision gitHubSCMRevision = (GitHubSCMRevision) revision;

        return Collections.singletonList(new CauseAction(gitHubSCMRevision.getCause()));
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMHead head,
                                           @CheckForNull SCMHeadEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveActions(jenkins.scm.api.SCMHead, jenkins.scm.api.SCMHeadEvent, hudson.model.TaskListener)");
        listener.getLogger().println(">> head " + head + " event " + event);

        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@CheckForNull SCMSourceEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveActions(jenkins.scm.api.SCMSourceEvent, hudson.model.TaskListener)");
        listener.getLogger().println(">> sourceEvent " + event);
//        return Collections.singletonList(getReposAction());
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
        return true;
    }

    @Nonnull
    @Override
    protected SCMProbe createProbe(@Nonnull SCMHead head,
                                   @CheckForNull SCMRevision revision) throws IOException {
        LOG.debug("CreateProbe");
        try {
            return fromSCMFileSystem(head, revision);
        } catch (InterruptedException e) {
            Throwables.propagate(e);
        }
        throw new IllegalStateException();
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
            return new SCMHeadCategory[]{BRANCH, PR, TAG};
        }


        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub source";
        }
    }
}
