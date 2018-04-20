package com.github.kostyasha.github.integration.multibranch;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.action.GitHubBranchAction;
import com.github.kostyasha.github.integration.multibranch.action.GitHubLinkAction;
import com.github.kostyasha.github.integration.multibranch.action.GitHubPRAction;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepoAction;
import com.github.kostyasha.github.integration.multibranch.action.GitHubSCMSourcesLocalStorage;
import com.github.kostyasha.github.integration.multibranch.action.GitHubTagAction;
import com.github.kostyasha.github.integration.multibranch.fs.GitHubSCMProbe;
import com.github.kostyasha.github.integration.multibranch.fs.TreeCache;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubSourceContext;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import com.github.kostyasha.github.integration.multibranch.repoprovider.GitHubRepoProvider2;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import com.github.kostyasha.github.integration.multibranch.scm.GitHubSCMFactory;
import com.github.kostyasha.github.integration.multibranch.scm.GitHubSCMFactory.GitHubSCMFactoryDescriptor;
import hudson.BulkChange;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.TaskListener;
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
import jenkins.scm.api.metadata.ObjectMetadataAction;
import jenkins.scm.api.metadata.PrimaryInstanceMetadataAction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.github.kostyasha.github.integration.multibranch.category.GitHubBranchSCMHeadCategory.BRANCH;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubPRSCMHeadCategory.PR;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubTagSCMHeadCategory.TAG;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class GitHubSCMSource extends SCMSource {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubSCMSource.class);
    /**
     * This will the URL to the project main branch. One for PRs and branches... etc...
     */
    private String projectUrlStr;

    // one for tags, etc
    private GitHubRepoProvider2 repoProvider = null;

    private List<GitHubHandler> handlers = new ArrayList<>();

    private GitHubSCMFactory scmFactory;

    private transient volatile GitHubSCMSourcesLocalStorage localStorage;

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

    public GitHubSCMFactory getScmFactory() {
        return scmFactory;
    }

    @DataBoundSetter
    public GitHubSCMSource setScmFactory(GitHubSCMFactory scmFactory) {
        this.scmFactory = scmFactory;
        return this;
    }

    public GitHubRepo getLocalRepo() {
        return getLocalStorage().getLocalRepo();
    }

    protected synchronized GitHubSCMSourcesLocalStorage getLocalStorage() {
        if (isNull(localStorage)) {
            localStorage = new GitHubSCMSourcesLocalStorage(getOwner(), getId());
            try {
                localStorage.load();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return localStorage;
    }

    @Override
    protected void retrieve(SCMSourceCriteria scmSourceCriteria, @Nonnull SCMHeadObserver scmHeadObserver,
                            SCMHeadEvent<?> scmHeadEvent, // null for manual run
                            @Nonnull TaskListener taskListener) throws IOException, InterruptedException {

        try (BulkChange bc = new BulkChange(getLocalStorage());
             TreeCache.Context ctx = TreeCache.createContext()) {
            GitHubRepo localRepo = getLocalRepo();

            // latch onto local repo state
            synchronized (localRepo) {
                localRepo.actualize(getRemoteRepo());

                GitHubSourceContext context = new GitHubSourceContext(this, scmHeadObserver, scmSourceCriteria,
                        scmHeadEvent, localRepo, getRemoteRepo(), taskListener);

                getHandlers().forEach(handler -> {
                    try {
                        handler.handle(context);
                    } catch (Throwable e) {
                        LOG.error("Can't process handler", e);
                        e.printStackTrace(taskListener.getLogger());
                    }
                });
            }

            bc.commit();
        }
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
        return scmFactory.createScm(this, scmHead, scmRevision);
    }

    // It is so hard to implement good APIs...
    @Nonnull
    @Override
    public Set<SCMRevision> parentRevisions(@Nonnull SCMHead head,
                                            @Nonnull SCMRevision revision,
                                            @CheckForNull TaskListener listener)
            throws IOException, InterruptedException {
        return super.parentRevisions(head, revision, listener);
    }

    @Nonnull
    @Override
    public Map<SCMHead, SCMRevision> parentHeads(@Nonnull SCMHead head, @CheckForNull TaskListener listener)
            throws IOException, InterruptedException {
        return super.parentHeads(head, listener);
    }

    @Nonnull
    @Override
    protected Set<SCMHead> retrieve(@Nonnull TaskListener listener) throws IOException, InterruptedException {
        return super.retrieve(listener);
    }

    @Nonnull
    @Override
    protected Set<SCMHead> retrieve(@CheckForNull SCMSourceCriteria criteria, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        return super.retrieve(criteria, listener);
    }

    @Override
    protected SCMRevision retrieve(@Nonnull SCMHead head, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        return super.retrieve(head, listener);
    }

    @Override
    protected SCMRevision retrieve(@Nonnull String thingName, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        return super.retrieve(thingName, listener);
    }

    @Nonnull
    @Override
    protected Set<String> retrieveRevisions(@Nonnull TaskListener listener) throws IOException, InterruptedException {
        return super.retrieveRevisions(listener);
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMRevision revision, @CheckForNull SCMHeadEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        GitHubSCMRevision gitHubSCMRevision = (GitHubSCMRevision) revision;
        GitHubCause<?> cause = gitHubSCMRevision.getCause();
        if (nonNull(cause)) {
            List<ParameterValue> params = new ArrayList<>();
            List<String> safeParams = new ArrayList<>();
            cause.fillParameters(params);
            params.forEach(p -> safeParams.add(p.getName()));
            return Arrays.asList(new CauseAction(cause), new ParametersAction(params, safeParams));
        }

        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMHead head,
                                           @CheckForNull SCMHeadEvent event,
                                           @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        List<Action> actions = new ArrayList<>();

        GHRepository remoteRepo = getRemoteRepo();

        boolean primary = false;
        GitHubLinkAction link = null;
        String desc = null;

        if (head instanceof GitHubBranchSCMHead) {
            // mark default branch item as primary
            primary = remoteRepo.getDefaultBranch().equals(head.getName());
            link = new GitHubBranchAction(remoteRepo, head.getName());
            desc = null;
        } else if (head instanceof GitHubTagSCMHead) {
            link = new GitHubTagAction(remoteRepo, head.getName());
            desc = null;
        } else if (head instanceof GitHubPRSCMHead) {
            GitHubPRSCMHead prHead = (GitHubPRSCMHead) head;
            link = new GitHubPRAction(remoteRepo, prHead.getPrNumber());
            desc = remoteRepo.getPullRequest(prHead.getPrNumber()).getTitle();
        }

        if (nonNull(link)) {
            actions.add(link);
        }

        actions.add(new ObjectMetadataAction(null, desc, isNull(link) ? null : link.getUrlName()));
        if (primary) {
            actions.add(new PrimaryInstanceMetadataAction());
        }

        return actions;
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@CheckForNull SCMSourceEvent event, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        return Collections.singletonList(new GitHubRepoAction(getRemoteRepo()));
    }

    @Nonnull
    @Override
    public SCMRevision getTrustedRevision(@Nonnull SCMRevision revision, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {
        return super.getTrustedRevision(revision, listener);
    }

    @Override
    public boolean canProbe() {
        return true;
    }

    @Nonnull
    @Override
    protected SCMProbe createProbe(@Nonnull SCMHead head, @CheckForNull SCMRevision revision) throws IOException {
        return new GitHubSCMProbe(this, (GitHubSCMHead<?>) head, (GitHubSCMRevision) revision);
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
            // array? bundled in descriptor?? seriously?
            return new SCMHeadCategory[]{BRANCH, PR, TAG};
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub source";
        }

        public List<GitHubSCMFactoryDescriptor> getScmFactoryDescriptors() {
            return ExtensionList.lookup(GitHubSCMFactoryDescriptor.class);
        }
    }

}
