package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

/**
 * Used to skip PR builder. Use case is skipping if special description exists.
 */
public class GitHubPRDescriptionEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Description matched to pattern";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRDescriptionEvent.class);

    private final String skipMsg;

    @DataBoundConstructor
    public GitHubPRDescriptionEvent(String skipMsg) {
        this.skipMsg = skipMsg;
    }

    /**
     * Checks for skip message in pull request description.
     *
     * @param remotePR {@link org.kohsuke.github.GHIssue} that contains description for checking
     */
    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger,
                               @Nonnull GHPullRequest remotePR,
                               @Nullable GitHubPRPullRequest localPR,
                               TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        GitHubPRCause cause = null;

        String pullRequestBody = remotePR.getBody().trim();
        LOG.debug("Job: '{}', trigger event: '{}', body for test: '{}'",
                gitHubPRTrigger.getJob().getFullName(),
                DISPLAY_NAME,
                escapeJava(pullRequestBody)
        );

        if (StringUtils.isNotBlank(pullRequestBody)) {
            HashSet<String> skipBuildPhrases = new HashSet<>(Arrays.asList(getSkipMsg().split("[\\r\\n]+")));
            skipBuildPhrases.remove("");
            LOG.debug("Job: '{}', trigger event: '{}', skipBuildPhrases: '{}'",
                    gitHubPRTrigger.getJob().getFullName(),
                    DISPLAY_NAME,
                    skipBuildPhrases
            );

            for (String skipBuildPhrase : skipBuildPhrases) {
                skipBuildPhrase = skipBuildPhrase.trim();
                Pattern skipBuildPhrasePattern = Pattern.compile(skipBuildPhrase);
                if (skipBuildPhrasePattern.matcher(pullRequestBody).matches()) {
                    LOG.info("Job: '{}', trigger event: '{}', PR body '{}' matches skipBuildPhrase '{}'.",
                            gitHubPRTrigger.getJob().getFullName(),
                            DISPLAY_NAME,
                            escapeJava(pullRequestBody),
                            skipBuildPhrase
                    );
                    logger.println(DISPLAY_NAME + ": Pull request description contains " + skipBuildPhrase + ", skipping");
                    cause = new GitHubPRCause(remotePR, "Pull request description contains " + skipBuildPhrase + ", skipping", true);
                    break;
                } else {
                    LOG.trace("Job: '{}', trigger event: '{}', phrase: '{}' didn't match to '{}'",
                            gitHubPRTrigger.getJob().getFullName(),
                            DISPLAY_NAME,
                            skipBuildPhrase,
                            escapeJava(pullRequestBody)
                    );
                }
            }
        }

        return cause;
    }

    public String getSkipMsg() {
        return skipMsg;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
