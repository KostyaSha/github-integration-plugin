package org.jenkinsci.plugins.github.pullrequest.utils;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.google.common.base.Function;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import hudson.model.Job;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class PRHelperFunctions {
    private static final Logger LOGGER = LoggerFactory.getLogger(PRHelperFunctions.class);

    private PRHelperFunctions() {
    }

    public static String asFullRepoName(GitHubRepositoryName repo) {
        checkNotNull(repo, "Can't get full name from null repo name");
        return String.format("%s/%s", repo.getUserName(), repo.getRepositoryName());
    }

    /**
     * Generates the name to use for a commit status context. Combines the GitHub
     * "display name" field with the name provided for the status update action build step
     * to produce the name to assign to the commit status context (what shows up in GitHub
     * as the name of the "check").
     *
     * Note that either or both the display name or the step name can be empty.
     * <ul>
     *     <li>If both are non-empty, the name is the combination of both.</li>
     *     <li>If only one is non-empty, it is used for the context name.</li>
     *     <li>If both are empty, the context name will default to the job name.</li>
     * </ul>
     */
    public static String getCommitStatusContext(Job<?, ?> job, @Nullable String stepName) {

        GithubProjectProperty githubSettings = job.getProperty(GithubProjectProperty.class);
        String jobContext = githubSettings.getDisplayName();

        StringBuilder contextBuilder = new StringBuilder();
        if (isNotBlank(jobContext)) {
            contextBuilder.append(jobContext.trim());
        }
        if (isNotBlank(stepName)) {
            if (0 != contextBuilder.length()) {
                contextBuilder.append('-');
            }
            contextBuilder.append(stepName.trim());
        }
        if (0 == contextBuilder.length()) {
            return job.getFullName();
        }
        return contextBuilder.toString();
    }

    public static Function<Integer, GHPullRequest> fetchRemotePR(final GHRepository ghRepository) {
        return new FetchRemotePRFunction(ghRepository);
    }

    public static Function<GHPullRequest, Integer> extractPRNumber() {
        return new ExtractPRNumberFunction();
    }

    private static class FetchRemotePRFunction implements Function<Integer, GHPullRequest> {
        private final GHRepository ghRepository;

        FetchRemotePRFunction(GHRepository ghRepository) {
            this.ghRepository = ghRepository;
        }

        @Override
        public GHPullRequest apply(Integer input) {
            try {
                return ghRepository.getPullRequest(input);
            } catch (IOException e) {
                LOGGER.error("Can't fetch pr by num {}", input, e);
                return null;
            }
        }
    }

    private static class ExtractPRNumberFunction extends NullSafeFunction<GHPullRequest, Integer> {
        @Override
        protected Integer applyNullSafe(@Nonnull GHPullRequest input) {
            return input.getNumber();
        }
    }

}
