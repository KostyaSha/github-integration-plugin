package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.generic.GitHubBranchDecisionContext;
import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.kostyasha.github.integration.generic.GitHubBranchDecisionContext.newGitHubBranchDecisionContext;
import static java.util.Objects.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class BranchToCauseConverter implements Function<GHBranch, GitHubBranchCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchToCauseConverter.class);

    private final GitHubBranchRepository localBranches;
    private final TaskListener listener;
    @CheckForNull
    private final GitHubBranchTrigger trigger;
    @CheckForNull
    private final GitHubBranchHandler handler;
    private final GitHubSCMSource source;

    private BranchToCauseConverter(@Nonnull GitHubBranchRepository localBranches,
                                   @Nonnull TaskListener listener,
                                   @Nonnull GitHubBranchTrigger trigger) {
        this.localBranches = localBranches;
        this.listener = listener;
        this.trigger = trigger;
        this.handler = null;
        this.source = null;
    }

    public BranchToCauseConverter(@Nonnull GitHubBranchRepository localBranches,
                                  @Nonnull TaskListener listener,
                                  @Nonnull GitHubBranchHandler handler,
                                  @Nonnull GitHubSCMSource source) {
        this.localBranches = localBranches;
        this.listener = listener;
        this.handler = handler;
        this.source = source;
        this.trigger = null;
    }

    public static BranchToCauseConverter toGitHubBranchCause(GitHubBranchRepository localRepo,
                                                             TaskListener listener,
                                                             GitHubBranchTrigger trigger) {
        return new BranchToCauseConverter(localRepo, listener, trigger);
    }

    public static BranchToCauseConverter toGitHubBranchCause(GitHubBranchRepository localRepo,
                                                             TaskListener listener,
                                                             @Nonnull GitHubBranchHandler handler,
                                                             @Nonnull GitHubSCMSource source) {
        return new BranchToCauseConverter(localRepo, listener, handler, source);
    }

    private List<GitHubBranchEvent> getEvents() {
        if (nonNull(trigger)) {
            return trigger.getEvents();
        } else if (nonNull(handler)) {
            return handler.getEvents();
        }

        return Collections.emptyList();
    }

    @Override
    public GitHubBranchCause apply(final GHBranch remoteBranch) {
        String branchName = remoteBranch.getName();
        GitHubBranch localBranch = localBranches.getBranches().get(branchName);

        GitHubBranchDecisionContext context = newGitHubBranchDecisionContext()
                .withListener(listener)
                .withLocalRepo(localBranches)
                .withRemoteBranch(remoteBranch)
                .withLocalBranch(localBranch)
                .withBranchTrigger(trigger)
                .withBranchHandler(handler)
                .withSCMSource(source)
                .build();
        
        List<GitHubBranchCause> causes = getEvents().stream()
                .map(event -> toCause(event, context))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String name = remoteBranch.getName();
        if (causes.isEmpty()) {
            LOGGER.debug("No build trigger causes found for branch [{}].", name);
            return null;
        }

        LOGGER.debug("All matched events for branch [{}] : {}.", name, causes);

        GitHubBranchCause cause = GitHubCause.skipTrigger(causes);
        if (cause != null) {
            LOGGER.debug("Cause [{}] indicated build should be skipped.", cause);
            listener.getLogger().println(String.format("Build of branch %s skipped: %s.", name, cause.getReason()));
            return null;
        } else if (!causes.isEmpty()) {
            // use the first cause from the list
            cause = causes.get(0);
            LOGGER.debug("Using build cause [{}] as trigger for branch [{}].", cause, name);
        }

        return cause;
    }

    private GitHubBranchCause toCause(GitHubBranchEvent event, GitHubBranchDecisionContext context) {
        try {
            return context.checkEvent(event);
        } catch (IOException e) {
            String branch = null;
            if (nonNull(context.getLocalBranch())){
                branch = context.getLocalBranch().getName();
            } else if (nonNull(context.getRemoteBranch())) {
                branch = context.getRemoteBranch().getName();
            }

            LOGGER.error("Event check failed, skipping branch '{}'.", branch, e);
            listener.error("Event check failed, skipping branch '{}' {}", branch, e);
            return null;
        }
    }

}
