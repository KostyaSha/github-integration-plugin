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

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Used to skip PR builder. Use case is skipping if special description exists.
 */
public class GitHubPRDescriptionEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Description matched to pattern";
    private final static Logger LOGGER = Logger.getLogger(GitHubPRDescriptionEvent.class.getName());

    private final String skipMsg;
    private boolean skip = true;

    @DataBoundConstructor
    public GitHubPRDescriptionEvent(String skipMsg, boolean skip) {
        this.skipMsg = skipMsg;
        this.skip = skip;
    }

    /**
     * Checks for skip message in pull request description.
     *
     * @param remotePR {@link org.kohsuke.github.GHIssue} that contains description for checking
     */
    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR, @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        GitHubPRCause cause = null;

        String pullRequestBody = remotePR.getBody().trim();
        if (StringUtils.isNotBlank(pullRequestBody)) {
            HashSet<String> skipBuildPhrases = new HashSet<String>(Arrays.asList(getSkipMsg().split("[\\r\\n]+")));
            skipBuildPhrases.remove("");

            for (String skipBuildPhrase : skipBuildPhrases) {
                skipBuildPhrase = skipBuildPhrase.trim();
                Pattern skipBuildPhrasePattern = Pattern.compile(skipBuildPhrase);
                if (skipBuildPhrasePattern.matcher(pullRequestBody).matches()) {
                    LOGGER.log(Level.INFO, "Pull request description with {0} skipBuildPhrase. Hence skipping the buildAndComment.",
                            skipBuildPhrase);
                    logger.println(DISPLAY_NAME + ": Pull request description contains " + skipBuildPhrase + ", skipping");
                    cause = new GitHubPRCause(remotePR, "Pull request description contains " + skipBuildPhrase + ", skipping",
                            isSkip());
                    break;
                }
            }
        }

        return cause;
    }

    public boolean isSkip() {
        return skip;
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
