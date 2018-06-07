package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.hooks.GitHubPullRequestScmHeadEvent;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.webhook.PullRequestInfo;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.execute;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter.toGitHubPRCause;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipPRInBadState.badState;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.ioOptStream;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.iof;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.iop;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRHandler extends GitHubHandler {

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

        TaskListener listener = context.getListener();
        GHRepository remoteRepo = context.getRemoteRepo();
        GitHubPRRepository localRepo = Objects.requireNonNull(context.getLocalRepo().getPrRepository());
        GitHubPRRepository oldRepo = new GitHubPRRepository(remoteRepo);
        oldRepo.getPulls().putAll(localRepo.getPulls());

        // prepare for run and fetch remote prs
        Stream<GHPullRequest> pulls;
        if (prNumber != null) {
            listener.getLogger().println("**** Processing pull request #" + prNumber + " ****");
            pulls = ioOptStream(() -> execute(() -> remoteRepo.getPullRequest(prNumber)));
            localRepo.getPulls().remove(prNumber);
        } else {
            listener.getLogger().println("**** Processing all pull requests ****");
            pulls = fetchRemotePRs(localRepo, remoteRepo);
            localRepo.getPulls().clear();
        }

        processCauses(context, pulls
                // filter out uninteresting
                .filter(iop(pr -> context.checkCriteria(new GitHubPRCause(pr, localRepo, "Check", false))))
                // update local state
                .map(updateLocalRepo(localRepo))
                // filter out bad ones
                .filter(badState(localRepo, listener))
                // TODO: move this logic to an event?
                // .filter(notUpdated(localRepo, listener))
                // create causes
                .map(toCause(context, oldRepo)));

        listener.getLogger().println("**** Done processing pull requests ****");
    }

    private Function<GHPullRequest, GitHubPRCause> toCause(GitHubSourceContext context, GitHubPRRepository localRepo) {
        TaskListener listener = context.getListener();
        GitHubSCMSource source = context.getSource();
        return pr -> {
            GitHubPRCause c = toGitHubPRCause(localRepo, listener, this, source).apply(pr);
            if (c == null) {
                c = new GitHubPRCause(pr, localRepo, "Skip", true);
            }
            return c;
        };
    }

    private static Stream<GHPullRequest> fetchRemotePRs(GitHubPRRepository localRepo, GHRepository remoteRepo) throws IOException {
        // fetch open prs
        Map<Integer, GHPullRequest> remotePulls = execute(() -> remoteRepo.getPullRequests(GHIssueState.OPEN)).stream()
                .collect(Collectors.toMap(GHPullRequest::getNumber, Function.identity()));

        // collect closed pull requests we knew of previously
        Stream<GHPullRequest> closed = new HashSet<>(localRepo.getPulls().keySet()).stream()
                .filter(pr -> !remotePulls.containsKey(pr))
                .map(iof(remoteRepo::getPullRequest));

        return Stream.concat(remotePulls.values().stream(), closed);
    }

    @Symbol("PullRequest")
    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "GitHub PR Handler";
        }
    }
}
