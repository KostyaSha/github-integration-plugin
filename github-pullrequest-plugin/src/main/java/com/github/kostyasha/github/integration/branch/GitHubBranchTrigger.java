package com.github.kostyasha.github.integration.branch;

import antlr.ANTLRException;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;
import com.github.kostyasha.github.integration.branch.trigger.JobRunnerForBranchCause;
import com.github.kostyasha.github.integration.branch.utils.ItemHelpers;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import com.github.kostyasha.github.integration.generic.GitHubTriggerDescriptor;
import com.github.kostyasha.github.integration.generic.errors.impl.GitHubHookRegistrationError;
import hudson.Extension;
import hudson.model.Job;
import hudson.triggers.Trigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.kostyasha.github.integration.branch.trigger.check.BranchToCauseConverter.toGitHubBranchCause;
import static com.github.kostyasha.github.integration.branch.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static com.github.kostyasha.github.integration.branch.trigger.check.SkipFirstRunForBranchFilter.ifSkippedFirstRun;
import static com.github.kostyasha.github.integration.branch.webhook.WebhookInfoBranchPredicates.withHookTriggerMode;
import static com.google.common.base.Charsets.UTF_8;
import static java.text.DateFormat.getDateTimeInstance;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.LIGHT_HOOKS;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTrigger extends GitHubTrigger<GitHubBranchTrigger> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchTrigger.class);
    public static final String FINISH_MSG = "Finished GitHub Branch trigger check";

    private List<GitHubBranchEvent> events = new ArrayList<>();

    private boolean preStatus = false;

    @CheckForNull
    private GitHubPRUserRestriction userRestriction;
    @CheckForNull
    private GitHubPRBranchRestriction branchRestriction;

    @CheckForNull
    private transient GitHubBranchPollingLogAction pollingLogAction;

    @Override
    public String getFinishMsg() {
        return FINISH_MSG;
    }

    /**
     * For groovy UI
     */
    @Restricted(value = NoExternalUse.class)
    public GitHubBranchTrigger() throws ANTLRException {
        super("");
    }

    @DataBoundConstructor
    public GitHubBranchTrigger(String spec, GitHubPRTriggerMode triggerMode, List<GitHubBranchEvent> events)
            throws ANTLRException {
        super(spec, triggerMode);
        this.events = events;
    }

    @DataBoundSetter
    public void setPreStatus(boolean preStatus) {
        this.preStatus = preStatus;
    }

    public boolean isPreStatus() {
        return preStatus;
    }

    @Nonnull
    public List<GitHubBranchEvent> getEvents() {
        return nonNull(events) ? events : emptyList();
    }

    @CheckForNull
    public void setEvents(List<GitHubBranchEvent> events) {
        this.events = events;
    }

    @Override
    public void start(Job item, boolean newInstance) {
        LOG.info("Starting GitHub Branch trigger for project {}", item.getFullName());
        super.start(item, newInstance);
        if (newInstance && getRepoProvider().isManageHooks(this) && withHookTriggerMode().apply(item)) {
            try {
                getRepoProvider().registerHookFor(this);
                getErrorsAction().removeErrors(GitHubHookRegistrationError.class);
            } catch (Throwable error) {
                getErrorsAction().addOrReplaceError(new GitHubHookRegistrationError(
                        String.format("Failed register hook for %s. <br/> Because %s",
                                item.getFullName(), error.toString())
                ));
                throw error;
            }
        }
    }

    @Override
    public void run() {
        if (getTriggerMode() != LIGHT_HOOKS) {
            doRun(null);
        }
    }

    @Override
    @CheckForNull
    public GitHubBranchPollingLogAction getPollingLogAction() {
        if (isNull(pollingLogAction) && nonNull(job)) {
            pollingLogAction = new GitHubBranchPollingLogAction(job);
        }

        return pollingLogAction;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * For running from external places. Goes to queue.
     */
    public void queueRun(Job<?, ?> job, final String branch) {
        this.job = job;
        getDescriptor().getQueue().execute(() -> doRun(branch));
    }

    /**
     * Runs check
     *
     * @param branch - branch for check, if null - then all PRs
     */
    public void doRun(String branch) {
        if (!ItemHelpers.isBuildable().test(job)) {
            LOG.debug("Job {} is disabled, but trigger run!", isNull(job) ? "<no job>" : job.getFullName());
            return;
        }

        if (!isSupportedTriggerMode(getTriggerMode())) {
            LOG.warn("Trigger mode {} is not supported yet ({})", getTriggerMode(), job.getFullName());
            return;
        }

        GitHubBranchRepository localRepository = ItemHelpers.getBranchRepositoryFor(job);
        if (isNull(localRepository)) {
            LOG.warn("Can't get repository info, maybe project {} misconfigured?", job.getFullName());
            return;
        }

        List<GitHubBranchCause> causes;

        try (LoggingTaskListenerWrapper listener =
                     new LoggingTaskListenerWrapper(getPollingLogAction().getPollingLogFile(), UTF_8)) {
            long startTime = System.currentTimeMillis();
            listener.debug("Running GitHub Branch trigger check for {} on {}",
                    getDateTimeInstance().format(new Date(startTime)), localRepository.getFullName());

            try {
                localRepository.actualise(getRemoteRepository(), listener);

                causes = readyToBuildCauses(localRepository, listener, branch);

                localRepository.saveQuietly();

                // TODO print triggering to listener?
                from(causes).filter(new JobRunnerForBranchCause(job, this)).toSet();
            } catch (Throwable t) {
                listener.error("Can't end trigger check: '{}'", t);
            }

            long duration = System.currentTimeMillis() - startTime;
            listener.info(FINISH_MSG + " for {} at {}. Duration: {}ms",
                    localRepository.getFullName(), getDateTimeInstance().format(new Date()), duration);
        } catch (Exception e) {
            LOG.error("Can't process check: {}", e);
        }
    }

    /**
     * @return list of causes for scheduling branch builds.
     */
    private List<GitHubBranchCause> readyToBuildCauses(GitHubBranchRepository localRepository,
                                                       LoggingTaskListenerWrapper listener,
                                                       @Nullable String requestedBranch) {
        try {
            GitHub github = getRepoProvider().getGitHub(this);
            if (isNull(github)) {
                LOG.error("GitHub connection is null, check Repo Providers!");
                throw new IllegalStateException("GitHub connection is null, check Repo Providers!");
            }

            GHRateLimit rateLimitBefore = github.getRateLimit();
            listener.debug("GitHub rate limit before check: {}", rateLimitBefore);

            // get local and remote list of branches
            GHRepository remoteRepo = getRemoteRepository();
            Set<GHBranch> remoteBranches = branchesToCheck(requestedBranch, remoteRepo, localRepository);

            List<GitHubBranchCause> causes = checkBranches(remoteBranches, localRepository, listener);

            /*
             * update details about the local repo after the causes are determined as they expect
             * new branches to not be found in the local details
             */
            updateLocalRepository(requestedBranch, remoteBranches, localRepository);
            saveIfSkipFirstRun();

            GHRateLimit rateLimitAfter = github.getRateLimit();
            int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
            LOG.info("GitHub rate limit after check {}: {}, consumed: {}, checked branches: {}",
                    localRepository.getFullName(), rateLimitAfter, consumed, remoteBranches.size());

            return causes;
        } catch (IOException e) {
            listener.error("Can't get build causes: '{}'", e);
            return emptyList();
        }
    }

    /**
     * Remote branch for future analysing. null - all remote branches.
     */
    private Set<GHBranch> branchesToCheck(@CheckForNull String branch, @Nonnull GHRepository remoteRepo,
                                          GitHubBranchRepository localRepository)
            throws IOException {
        final LinkedHashSet<GHBranch> ghBranches = new LinkedHashSet<>();

        if (branch != null) {
            try {
                GHBranch ghBranch = remoteRepo.getBranch(branch);
                if (ghBranch != null) {
                    ghBranches.add(ghBranch);
                }
            } catch (FileNotFoundException ignore) {
            }

        } else {
            ghBranches.addAll(remoteRepo.getBranches().values());
        }

        return ghBranches;
    }

    private List<GitHubBranchCause> checkBranches(Set<GHBranch> remoteBranches,
                                                  GitHubBranchRepository localRepository, LoggingTaskListenerWrapper listener) {
        List<GitHubBranchCause> causes = remoteBranches.stream()
                // TODO: update user whitelist filter
                .filter(ifSkippedFirstRun(listener, skipFirstRun))
                .filter(Objects::nonNull)
                .map(toGitHubBranchCause(localRepository, listener, this))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LOG.debug("Build trigger count for [{}] : {}", localRepository.getFullName(), causes.size());
        return causes;
    }

    public static void updateLocalRepository(@CheckForNull String requestedBranch,
                                             Set<GHBranch> remoteBranches, GitHubBranchRepository localRepository) {
        //refresh checked local state for checked branches
        long count = remoteBranches.stream()
                .map(updateLocalRepo(localRepository))
                .count();
        LOG.trace("Updated local branch details with [{}] repositories.", count);

        // remove deleted branches from local state
        Map<String, GitHubBranch> localBranches;
        if (nonNull(requestedBranch)) {
            localBranches = new HashMap<>();
            if (localRepository.getBranches().containsKey(requestedBranch)) {
                localBranches.put(requestedBranch, localRepository.getBranches().get(requestedBranch));
            }
        } else {
            localBranches = localRepository.getBranches();
        }

        final Iterator<String> iterator = localBranches.keySet().iterator();
        while (iterator.hasNext()) {
            final String localBranch = iterator.next();
            final boolean present = remoteBranches.stream()
                    .filter(br -> br.getName().equals(localBranch))
                    .findFirst()
                    .isPresent();
            if (!present) {
                LOG.debug("Removing {}, from localBranches", localBranch);
                iterator.remove();
            }
        }
    }

    private static boolean isSupportedTriggerMode(GitHubPRTriggerMode mode) {
        return mode != LIGHT_HOOKS;
    }

    @Extension
    public static class DescriptorImpl extends GitHubTriggerDescriptor {

        public DescriptorImpl() {
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub Branches";
        }

        // list all available descriptors for choosing in job configuration
        public List<GitHubBranchEventDescriptor> getEventDescriptors() {
            return GitHubBranchEventDescriptor.all();
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }
    }
}
