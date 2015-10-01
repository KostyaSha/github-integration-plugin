package org.jenkinsci.plugins.github.pullrequest.utils;

import com.google.common.base.Function;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class PRHelperFunctions {
    private static final Logger LOGGER = LoggerFactory.getLogger(PRHelperFunctions.class);

    private PRHelperFunctions() {
    }

    public static Function<Integer, GHPullRequest> fetchRemotePR(final GHRepository ghRepository) {
        return new FetchRemotePRFunction(ghRepository);
    }

    public static Function<GHPullRequest, Integer> extractPRNumber() {
        return new ExtractPRNumberFunction();
    }

    private static class FetchRemotePRFunction implements Function<Integer, GHPullRequest> {
        private final GHRepository ghRepository;

        public FetchRemotePRFunction(GHRepository ghRepository) {
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
