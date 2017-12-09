package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMHeadObserver;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.execute;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static java.util.Collections.singleton;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.BranchRestrictionFilter.withBranchRestriction;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.NotUpdatedPRFilter.notUpdated;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter.toGitHubPRCause;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipFirstRunForPRFilter.ifSkippedFirstRun;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipPRInBadState.badState;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.UserRestrictionFilter.withUserRestriction;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.extractPRNumber;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.fetchRemotePR;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRHandler extends GitHubHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRHandler.class);

    @CheckForNull
    private List<GitHubPREvent> events = new ArrayList<>();

    @DataBoundConstructor
    public GitHubPRHandler() {
    }

    public List<GitHubPREvent> getEvents() {
        return events;
    }

    @DataBoundSetter
    public GitHubPRHandler setEvents(List<GitHubPREvent> events) {
        this.events = events;
        return this;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void handle(@Nonnull SCMHeadObserver observer,
                       @Nonnull GitHubRepo localPRs,
                       @Nonnull GHRepository remoteRepo,
                       @Nonnull TaskListener listener,
                       @Nonnull GitHubSCMSource source) throws IOException {
        GitHub github = source.getRepoProvider().getGitHub(source);

        GHRateLimit rateLimitBefore = github.getRateLimit();
        listener.getLogger().println("GitHub rate limit before check: " + rateLimitBefore);

        GitHubPRRepository prRepository = localPRs.getPrRepository();

        // get local and remote list of PRs
        Set<GHPullRequest> remotePulls = pullRequestsToCheck(null, remoteRepo, prRepository);

        Set<GHPullRequest> prepared = from(remotePulls)
                .filter(badState(prRepository, listener))
                .filter(notUpdated(prRepository, listener))
                .toSet();

        List<GitHubPRCause> causes = from(prepared)
                .filter(notNull())
//                    .filter(and(
//                            ifSkippedFirstRun(listener, isSkipFirstRun()),
//                            withBranchRestriction(listener, getBranchRestriction()),
//                            withUserRestriction(listener, getUserRestriction())
//                    ))
                .transform(new PullRequestToCauseConverter(prRepository, listener, this))
                .filter(notNull())
                .toList();

        LOG.trace("Causes count for {}: {}", prRepository.getFullName(), causes.size());

        // refresh all PRs because user may add events that may trigger unexpected builds.
        from(remotePulls).transform(updateLocalRepo(prRepository)).toSet();

//            saveIfSkipFirstRun();

        GHRateLimit rateLimitAfter = github.getRateLimit();
        int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
        LOG.info("GitHub rate limit after check {}: {}, consumed: {}, checked PRs: {}",
                localPRs.getUrlName(), rateLimitAfter, consumed, remotePulls.size());

        HashSet<String> causedPRs = new HashSet<>();

        causes.forEach(prCause -> {
            GitHubPRSCMHead scmHead = new GitHubPRSCMHead(prCause);
            AbstractGitSCMSource.SCMRevisionImpl scmRevision = new GitHubSCMRevision(scmHead, prCause.getHeadSha(), false, prCause);
            try {
                observer.observe(scmHead, scmRevision);
                causedPRs.add(scmHead.getName());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.getLogger());
            }
        });

        // don't think that items without cause are orphaned,
        // make com.cloudbees.hudson.plugins.folder.computed.ChildObserver.shouldUpdate() happy
        // or don't hide childObserver
        prRepository.getPulls().entrySet().stream()
                .filter(it -> !causedPRs.contains(it.getKey().toString()))
                .map(Map.Entry::getValue)
                .forEach(value -> {
                    try {
                        GitHubPRSCMHead scmHead = new GitHubPRSCMHead(Integer.toString(value.getNumber()));
                        GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, value.getHeadSha(), true, null);
                        observer.observe(scmHead, scmRevision);
                    } catch (IOException | InterruptedException e) {
                        // try as much as can
                        e.printStackTrace(listener.getLogger());
                    }
                });
    }

    /**
     * @return remote pull requests for future analysing.
     */
    private static Set<GHPullRequest> pullRequestsToCheck(@Nullable Integer prNumber,
                                                          @Nonnull GHRepository remoteRepo,
                                                          @Nonnull GitHubPRRepository localPRRepo) throws IOException {
        if (prNumber != null) {
            return execute(() -> singleton(remoteRepo.getPullRequest(prNumber)));
        } else {
            List<GHPullRequest> remotePulls = execute(() -> remoteRepo.getPullRequests(GHIssueState.OPEN));

            Set<Integer> remotePRNums = from(remotePulls).transform(extractPRNumber()).toSet();

            return from(localPRRepo.getPulls().keySet())
                    // add PRs that was closed on remote
                    .filter(not(in(remotePRNums)))
                    .transform(fetchRemotePR(remoteRepo))
                    .filter(notNull())
                    .append(remotePulls)
                    .toSet();
        }
    }

    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub PR Handler";
        }
    }
}
