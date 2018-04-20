package org.jenkinsci.plugins.github_integration.awaitility;

import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static java.lang.System.currentTimeMillis;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.loginToGithub;
import static java.util.Objects.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GHFromServerConfigAppeared implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GHFromServerConfigAppeared.class);
    private GitHubServerConfig gitHubServerConfig;
    final long timeBefore;

    public GHFromServerConfigAppeared(GitHubServerConfig gitHubServerConfig) {
        this.gitHubServerConfig = gitHubServerConfig;
        timeBefore = currentTimeMillis();
    }

    @Override
    public Boolean call() throws Exception {
        GitHub gitHub = loginToGithub().apply(gitHubServerConfig);
        if (nonNull(gitHub)) {
            LOG.debug("loginToGithub() delay {} ms.", currentTimeMillis() - timeBefore);
            return true;
        }

        throw new AssertionError("GitHub doesn't appear");
    }

    public static Callable<Boolean> ghAppeared(GitHubServerConfig gitHubServerConfig) {
        return new GHFromServerConfigAppeared(gitHubServerConfig);
    }
}

