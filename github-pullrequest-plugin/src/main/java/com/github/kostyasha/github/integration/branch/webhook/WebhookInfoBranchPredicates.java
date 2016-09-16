package com.github.kostyasha.github.integration.branch.webhook;

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import hudson.model.Job;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS_CRON;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.LIGHT_HOOKS;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.asFullRepoName;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;

/**
 * @author Kanstantsin Shautsou
 */
public class WebhookInfoBranchPredicates {
    private WebhookInfoBranchPredicates() {
    }

    public static Predicate<Job> withBranchTrigger() {
        return Predicates.and(
                withTrigger(GitHubBranchTrigger.class),
                withHookTriggerMode()
        );
    }

    public static Predicate<Job> withHookTriggerMode() {
        return new HookTriggerMode();
    }

    /**
     * Checks against repo extracted from BranchTrigger.
     */
    public static Predicate<Job> withBranchTriggerRepo(final String repo) {
        return new WithBranchTriggerRepo(repo);
    }

    private static class HookTriggerMode implements Predicate<Job> {
        @Override
        public boolean apply(Job job) {
            return Predicates.in(asList(HEAVY_HOOKS, HEAVY_HOOKS_CRON, LIGHT_HOOKS))
                    .apply(checkNotNull(
                            ghBranchTriggerFromJob(job),
                            "This predicate can be applied only for job with GitHubBranchTrigger"
                    ).getTriggerMode());
        }
    }

    private static class WithBranchTriggerRepo implements Predicate<Job> {
        private final String repo;

        WithBranchTriggerRepo(String repo) {
            this.repo = repo;
        }

        @Override
        public boolean apply(Job job) {
            return equalsIgnoreCase(
                    repo,
                    asFullRepoName(
                            ghBranchTriggerFromJob(job).getRepoFullName(job)
                    )
            );
        }
    }
}
