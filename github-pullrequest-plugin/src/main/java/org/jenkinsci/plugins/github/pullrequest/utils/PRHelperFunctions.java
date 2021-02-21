package org.jenkinsci.plugins.github.pullrequest.utils;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Function;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

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
        protected Integer applyNullSafe(@NonNull GHPullRequest input) {
            return input.getNumber();
        }
    }

}
