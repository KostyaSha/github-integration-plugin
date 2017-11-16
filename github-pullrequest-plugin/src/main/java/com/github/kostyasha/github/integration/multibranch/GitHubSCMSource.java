package com.github.kostyasha.github.integration.multibranch;

import com.cloudbees.hudson.plugins.folder.computed.ComputedFolder;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.generic.GitHubBadgeAction;
import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.action.GitHubSCMSourcesReposAction;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandler;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.repoprovider.GitHubPluginRepoProvider2;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import com.google.common.annotations.Beta;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import jenkins.plugins.git.AbstractGitSCMSource;
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
import org.jenkinsci.plugins.github.pullrequest.GitHubPRBadgeAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.kostyasha.github.integration.multibranch.category.GitHubBranchSCMHeadCategory.BRANCH;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubPRSCMHeadCategory.PR;
import static com.github.kostyasha.github.integration.multibranch.category.GitHubTagSCMHeadCategory.TAG;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;


public class GitHubSCMSource extends SCMSource {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubSCMSource.class);
    /**
     * This will the URL to the project main branch.
     * One for PRs and branches... etc...
     */
    private String projectUrlStr;

    // one for tags, etc
    private GitHubPluginRepoProvider2 repoProvider = null;

    private List<GitHubHandler> handlers = new ArrayList<>();


    private transient List<GitHubCause> causes = new ArrayList<>();

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
    public GitHubPluginRepoProvider2 getRepoProvider() {
        return repoProvider;
    }

    @DataBoundSetter
    public GitHubSCMSource setRepoProvider(GitHubPluginRepoProvider2 repoProvider) {
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
        ComputedFolder owner = (ComputedFolder) getOwner();

        GitHubSCMSourcesReposAction sourcesReposAction = owner.getAction(GitHubSCMSourcesReposAction.class);
        return sourcesReposAction.getOrCreate(this);
    }

    @Override
    protected void retrieve(SCMSourceCriteria scmSourceCriteria, //useless
                            @Nonnull SCMHeadObserver scmHeadObserver,
                            SCMHeadEvent<?> scmHeadEvent, // null for manual run
                            @Nonnull TaskListener taskListener) throws IOException, InterruptedException {
        PrintStream llog = taskListener.getLogger();
        llog.println("> GitHubSCMSource.retrieve(jenkins.scm.api.SCMSourceCriteria, jenkins.scm.api.SCMHeadObserver, jenkins.scm.api.SCMHeadEvent<?>, hudson.model.TaskListener)");
        llog.println(">> scmSourceCriteria " + scmSourceCriteria);
        llog.println(">> scmHeadObserver " + scmHeadObserver);
        llog.println(">> scmHeadEvent " + scmHeadEvent);

        GitHubRepo localRepo = getLocalRepo();

        // TODO actualise some repo for UI Action?
        localRepo.actualize(getRemoteRepo());

//        List<GitHubCause> causes = new ArrayList<>();

        getHandlers().forEach(handler -> {
            try {
                causes.addAll(handler.handle(localRepo, getRemoteRepo(), taskListener, this));
            } catch (IOException e) {
                LOG.error("Can't get remoteRepo()", e);
                e.printStackTrace(llog);
            }
        });

        causes.forEach(cause -> {
            if (cause instanceof GitHubBranchCause) {
                GitHubBranchCause branchCause = (GitHubBranchCause) cause;
                String commitSha = branchCause.getCommitSha();
                String branchName = branchCause.getBranchName();

                GitHubBranchSCMHead scmHead = new GitHubBranchSCMHead(branchName, branchCause);
                AbstractGitSCMSource.SCMRevisionImpl scmRevision = new GitHubSCMRevision(scmHead, commitSha);
                try {
                    scmHeadObserver.observe(scmHead, scmRevision);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace(taskListener.getLogger());
                }
            } else if (cause instanceof GitHubPRCause) {
                GitHubPRCause prCause = (GitHubPRCause) cause;
                GitHubPRSCMHead scmHead = new GitHubPRSCMHead(Integer.toString(prCause.getNumber()), prCause);
                AbstractGitSCMSource.SCMRevisionImpl scmRevision = new GitHubSCMRevision(scmHead, ((GitHubPRCause) cause).getHeadSha());
                try {
                    scmHeadObserver.observe(scmHead, scmRevision);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace(taskListener.getLogger());
                }
            }
        });
//        GitHubBranchSCMHead branchHead = new GitHubBranchSCMHead("someBranch");
//
//        scmHeadObserver.observe(branchHead, new SCMRevisionImpl(branchHead, UUID.randomUUID().toString()));
//
//
//        GitHubTagSCMHead taggy = new GitHubTagSCMHead("taggy");
//        scmHeadObserver.observe(taggy, new SCMRevisionImpl(taggy, UUID.randomUUID().toString()));

    }


    @Nonnull
    public GHRepository getRemoteRepo() throws IOException {
        GHRepository remoteRepository = getRepoProvider().getGHRepository(this);
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

        return super.retrieveActions(revision, event, listener);
    }

    @Nonnull
    @Override
    protected List<Action> retrieveActions(@Nonnull SCMHead head,
                                           @CheckForNull SCMHeadEvent event,
                                           @Nonnull TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("> GitHubSCMSource.retrieveActions(jenkins.scm.api.SCMHead, jenkins.scm.api.SCMHeadEvent, hudson.model.TaskListener)");
        listener.getLogger().println(">> head " + head + " event " + event);
        String name = head.getName();

        return Collections.singletonList(new CauseAction(((GitHubSCMHead) head).getCause()));
//        return super.retrieveActions(head, event, listener);
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
