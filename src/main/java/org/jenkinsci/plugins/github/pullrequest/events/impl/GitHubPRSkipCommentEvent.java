package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Used to skip PR builder. Use case is skipping if special comment exists.
 */
public class GitHubPRSkipCommentEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Skip comment";
    private final static Logger LOGGER = Logger.getLogger(GitHubPRSkipCommentEvent.class.getName());

    public String getSkipMsg() {
        return skipMsg;
    }

    private final String skipMsg;

    @DataBoundConstructor
    public GitHubPRSkipCommentEvent(String skipMsg) {
        this.skipMsg = skipMsg;
    }

    //TODO integration with hooks

    /**
     * Checks for skip buildAndComment phrase in pull request comment. If present it updates shouldRun as false.
     *
     * @param remotePR {@link org.kohsuke.github.GHIssue} that contains comments to check
     */
    @Override
    public boolean isSkip(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                          @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) {
        final PrintStream logger = listener.getLogger();

        boolean ret = false;
        String pullRequestBody = remotePR.getBody().trim();

        if (StringUtils.isNotBlank(pullRequestBody)) {
            HashSet<String> skipBuildPhrases = new HashSet<String>(Arrays.asList(getSkipMsg().split("[\\r\\n]+")));
            skipBuildPhrases.remove("");

            for (String skipBuildPhrase : skipBuildPhrases) {
                skipBuildPhrase = skipBuildPhrase.trim();
                Pattern skipBuildPhrasePattern = Pattern.compile(skipBuildPhrase);
                if (skipBuildPhrasePattern.matcher(pullRequestBody).matches()) {
                    LOGGER.log(Level.INFO, "Pull request commented with {0} skipBuildPhrase. Hence skipping the buildAndComment.",
                            skipBuildPhrase);
                    logger.println(DISPLAY_NAME + ": Pull request commented with " + skipBuildPhrase + ", skipping");
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
