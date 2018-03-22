package com.github.kostyasha.github.integration.multibranch.handler;

import static com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.execute;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static java.util.Collections.singleton;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.NotUpdatedPRFilter.notUpdated;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipPRInBadState.badState;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.extractPRNumber;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.fetchRemotePR;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter;
import org.jenkinsci.plugins.github.pullrequest.webhook.PullRequestInfo;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import com.github.kostyasha.github.integration.multibranch.hooks.GitHubPullRequestScmHeadEvent;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;

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
    public void handle(@Nonnull GitHubSourceContext context) throws IOException {

        Integer prNumber;

        SCMHeadEvent<?> scmHeadEvent = context.getScmHeadEvent();
        if (scmHeadEvent instanceof GitHubPullRequestScmHeadEvent) {
            PullRequestInfo info = (PullRequestInfo) scmHeadEvent.getPayload();
            prNumber = info.getNum();
        } else if (scmHeadEvent != null) {
            // not our event, skip completely
            return;
        } else {
            prNumber = null;
        }
        

        GitHubSCMSource source = context.getSource();
        GitHubRepo localRepo = context.getLocalRepo();
        GitHubPRRepository prRepository = localRepo.getPrRepository();
        GitHub github = context.getGitHub();
        TaskListener listener = context.getListener();

        if (prNumber != null) {
            listener.getLogger().println("**** Processing pull request #" + prNumber + " ****");
        } else {
            listener.getLogger().println("**** Processing pull requests ****");
        }

        GHRateLimit rateLimitBefore = github.getRateLimit();
        listener.getLogger().println("GitHub rate limit before check: " + rateLimitBefore);

        
        // get local and remote list of PRs
        Set<GHPullRequest> remotePulls = pullRequestsToCheck(prNumber, context);

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
                .transform(new PullRequestToCauseConverter(prRepository, listener, source, this))
                .filter(notNull())
                .toList();

        LOG.trace("Causes count for {}: {}", prRepository.getFullName(), causes.size());

        // refresh all PRs because user may add events that may trigger unexpected builds.
        if (prNumber != null) {
            prRepository.getPulls().remove(prNumber);
        } else {
            prRepository.getPulls().clear();
        }
        from(remotePulls).transform(updateLocalRepo(prRepository)).toSet();

//            saveIfSkipFirstRun();

        GHRateLimit rateLimitAfter = github.getRateLimit();
        int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
        LOG.info("GitHub rate limit after check {}: {}, consumed: {}, checked PRs: {}",
                localRepo.getUrlName(), rateLimitAfter, consumed, remotePulls.size());

        HashSet<Integer> causedPRs = new HashSet<>();

        causes.forEach(prCause -> {
            GitHubPRSCMHead scmHead = new GitHubPRSCMHead(prCause, source.getId());
            GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, prCause.getHeadSha(), false, prCause);
            try {
                context.getObserver().observe(scmHead, scmRevision);
                causedPRs.add(prCause.getNumber());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.getLogger());
            }
        });

        // notify untriggered items that we're still interested in them
        prRepository.getPulls().entrySet().stream()
                // only handle the requested pr if present
                .filter(it -> prNumber == null || prNumber.equals(it.getKey()))
                // and only if it wasn't triggered
                .filter(it -> !causedPRs.contains(it.getKey()))
                .map(Map.Entry::getValue)
                .forEach(value -> {
                    try {
                        GitHubPRSCMHead scmHead = new GitHubPRSCMHead(value.getNumber(), value.getBaseRef(), source.getId());
                        GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, value.getHeadSha(), true, null);
                        context.getObserver().observe(scmHead, scmRevision);
                    } catch (IOException | InterruptedException e) {
                        // try as much as can
                        e.printStackTrace(listener.getLogger());
                    }
                });
        listener.getLogger().println("**** Done processing pull requests ****\n");
    }

    /**
     * @return remote pull requests for future analysing.
     */
    private static Set<GHPullRequest> pullRequestsToCheck(@Nullable Integer prNumber, @Nonnull GitHubSourceContext context) throws IOException {
        return filterOutUninteresting(fetchRemotePRs(prNumber, context.getLocalRepo(), context.getRemoteRepo()), context);
    }

    private static Set<GHPullRequest> fetchRemotePRs(@Nullable Integer prNumber, GitHubRepo localRepo, GHRepository remoteRepo) throws IOException {
        if (prNumber != null) {
            return execute(() -> singleton(remoteRepo.getPullRequest(prNumber)));
        } else {
            List<GHPullRequest> remotePulls = execute(() -> remoteRepo.getPullRequests(GHIssueState.OPEN));

            Set<Integer> remotePRNums = from(remotePulls).transform(extractPRNumber()).toSet();

            return from(localRepo.getPrRepository().getPulls().keySet())
                    // add PRs that were closed on remote
                    .filter(not(in(remotePRNums)))
                    .transform(fetchRemotePR(remoteRepo))
                    .filter(notNull())
                    .append(remotePulls)
                    .toSet();
        }
    }

    private static Set<GHPullRequest> filterOutUninteresting(Set<GHPullRequest> prs, @Nonnull GitHubSourceContext context) throws IOException {
        Set<GHPullRequest> newPrs = new HashSet<>();
        for (GHPullRequest pr : prs) {
            if (isInteresting(pr, context)) {
                newPrs.add(pr);
            }
        }
        return newPrs;
    }

    private static boolean isInteresting(@Nonnull GHPullRequest pr, @Nonnull GitHubSourceContext context) throws IOException {
        GitHubPRSCMHead head = new GitHubPRSCMHead(pr.getNumber(), pr.getBase().getRef(), context.getSource().getId());
        return context.checkCriteria(head, new GitHubSCMRevision(head, pr.getBase().getSha(), true, null));
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
