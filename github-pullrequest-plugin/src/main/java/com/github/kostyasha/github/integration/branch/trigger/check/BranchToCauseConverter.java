package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Kanstantsin Shautsou
 */
public class BranchToCauseConverter implements Function<GHBranch, GitHubBranchCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchToCauseConverter.class);

    private final GitHubBranchRepository localRepo;
    private final LoggingTaskListenerWrapper listener;
    private final GitHubBranchTrigger trigger;

    private BranchToCauseConverter(GitHubBranchRepository localRepo,
                                   LoggingTaskListenerWrapper listener,
                                   GitHubBranchTrigger trigger) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.trigger = trigger;
    }

    public static BranchToCauseConverter toGitHubBranchCause(GitHubBranchRepository localRepo,
                                                             LoggingTaskListenerWrapper listener,
                                                             GitHubBranchTrigger trigger) {
        return new BranchToCauseConverter(localRepo, listener, trigger);
    }

    @Override
    public GitHubBranchCause apply(final GHBranch remoteBranch) {
        List<GitHubBranchCause> causes = trigger.getEvents().stream()
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
            listener.info("Build of branch [{}] skipped: {}.", name, cause.getReason());
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
        GitHubBranch localBranch = localRepo.getBranches().get(branchName);

        try {
            return event.check(trigger, remoteBranch, localBranch, localRepo, listener);
        } catch (IOException e) {
            LOGGER.error("Event check failed, skipping branch [{}].", branchName, e);
            listener.error("Event check failed, skipping branch [{}] {}", branchName, e);

            return null;
        }
    }
}
