package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
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

    private BranchToCauseConverter(@Nonnull GitHubBranchRepository localBranches,
                                   @Nonnull TaskListener listener,
                                   @Nonnull GitHubBranchTrigger trigger) {
        this.localBranches = localBranches;
        this.listener = listener;
        this.trigger = trigger;
        this.handler = null;
    }

    public BranchToCauseConverter(@Nonnull GitHubBranchRepository localBranches,
                                  @Nonnull TaskListener listener,
                                  @Nonnull GitHubBranchHandler handler) {
        this.localBranches = localBranches;
        this.listener = listener;
        this.handler = handler;
        this.trigger = null;
    }

    public static BranchToCauseConverter toGitHubBranchCause(GitHubBranchRepository localRepo,
                                                             TaskListener listener,
                                                             GitHubBranchTrigger trigger) {
        return new BranchToCauseConverter(localRepo, listener, trigger);
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
        List<GitHubBranchCause> causes = getEvents().stream()
                .map(event -> toCause(event, remoteBranch))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String name = remoteBranch.getName();
        if (causes.isEmpty()) {
            LOGGER.debug("No build trigger causes found for branch [{}].", name);
            return null;
        }

        LOGGER.debug("All matched events for branch [{}] : {}.", name, causes);

        GitHubBranchCause cause = skipTrigger(causes);
        if (cause != null) {
            listener.getLogger().println(String.format("Build of branch [{}] skipped: {}.", name, cause.getReason()));
            return null;
        }

        // use the first cause from the list
        cause = causes.get(0);
        LOGGER.debug("Using build cause [{}] as trigger for branch [{}].", cause, name);

        return cause;
    }

    private GitHubBranchCause skipTrigger(List<GitHubBranchCause> causes) {
        GitHubBranchCause cause = causes.stream()
                .filter(GitHubBranchCause::isSkip)
                .findFirst()
                .orElse(null);

        if (cause == null) {
            return null;
        }

        LOGGER.debug("Cause [{}] indicated build should be skipped.", cause);
        return cause;
    }

    private GitHubBranchCause toCause(GitHubBranchEvent event, GHBranch remoteBranch) {
        String branchName = remoteBranch.getName();
        GitHubBranch localBranch = localBranches.getBranches().get(branchName);

        try {
            if (nonNull(trigger)) {
                return event.check(trigger, remoteBranch, localBranch, localBranches, listener);
            } else {
                return event.check(handler, remoteBranch, localBranch, localBranches, listener);
            }
        } catch (IOException e) {
            LOGGER.error("Event check failed, skipping branch [{}].", branchName, e);
            listener.error("Event check failed, skipping branch [{}] {}", branchName, e);

            return null;
        }
    }

}
