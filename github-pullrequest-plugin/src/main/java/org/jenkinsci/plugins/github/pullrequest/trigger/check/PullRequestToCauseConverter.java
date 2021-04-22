package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler;
import com.google.common.base.Function;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static java.util.Objects.nonNull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class PullRequestToCauseConverter implements Function<GHPullRequest, GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestToCauseConverter.class);

    private final GitHubPRRepository localRepo;
    private final TaskListener listener;

    @CheckForNull
    private final GitHubPRTrigger trigger;

    @CheckForNull
    private GitHubSCMSource source;
    @CheckForNull
    private final GitHubPRHandler prHandler;

    private PullRequestToCauseConverter(GitHubPRRepository localRepo,
                                        TaskListener listener,
                                        GitHubPRTrigger trigger) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.trigger = trigger;
        prHandler = null;
    }

    public PullRequestToCauseConverter(@NonNull GitHubPRRepository localRepo,
                                       @NonNull TaskListener listener,
                                       @NonNull GitHubSCMSource source,
                                       @NonNull GitHubPRHandler prHandler) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.source = source;
        this.prHandler = prHandler;
        trigger = null;
    }

    public static PullRequestToCauseConverter toGitHubPRCause(@NonNull GitHubPRRepository localRepo,
                                                              @NonNull TaskListener listener,
                                                              @NonNull GitHubPRTrigger trigger) {
        return new PullRequestToCauseConverter(localRepo, listener, trigger);
    }

    public static PullRequestToCauseConverter toGitHubPRCause(@NonNull GitHubPRRepository localRepo,
                                                              @NonNull TaskListener listener,
                                                              @NonNull GitHubPRHandler prHandler,
                                                              @NonNull GitHubSCMSource source) {
        return new PullRequestToCauseConverter(localRepo, listener, source, prHandler);
    }

    /**
     * TODO migrate to java8 and cleanup.
     *
     * @return only real trigger cause if matched trigger (not skip) event found for this remotePr.
     */
    @CheckForNull
    @Override
    public GitHubPRCause apply(final GHPullRequest remotePR) {

        @CheckForNull
        GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());
        GitHubPRDecisionContext context = newGitHubPRDecisionContext()
                .withListener(listener)
                .withLocalPR(localPR)
                .withRemotePR(remotePR)
                .withLocalRepo(localRepo)
                .withPrTrigger(trigger)
                .withPrHandler(prHandler)
                .withSCMSource(source)
                .build();

        final List<GitHubPRCause> causes = getEvents().stream()
                .map(e -> toCause(e, context))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        GitHubPRCause cause = GitHubPRCause.skipTrigger(causes);

        if (cause != null) {
            LOGGER.debug("Cause [{}] indicated build should be skipped.", cause);
            listener.getLogger().println(String.format("Build of pr %s skipped: %s.", remotePR.getNumber(), cause.getReason()));
            return null;
        } else if (!causes.isEmpty()) {
            // use the first cause from the list
            cause = causes.get(0);
            LOGGER.debug("Using build cause [{}] as trigger for pr [{}].", cause, remotePR.getNumber());
        }

        return cause;
    }

    public List<GitHubPREvent> getEvents() {
        if (nonNull(trigger)) {
            return trigger.getEvents();
        } else if (nonNull(prHandler)) {
            return prHandler.getEvents();
        }

        throw new IllegalArgumentException("Can't extract events");
    }

    private GitHubPRCause toCause(GitHubPREvent event, GitHubPRDecisionContext context) {
        // null if local not existed before
        try {
            return context.checkEvent(event);
        } catch (IOException e) {
            LOGGER.warn("Can't check trigger event", e);
            listener.error("Can't check trigger event, so skipping PR #{}. {}", context.getRemotePR().getNumber(), e);
            return null;
        }
    }

}
