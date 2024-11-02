package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;
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
     * @param prDecisionContext.remotePR {@link org.kohsuke.github.GHIssue} that contains description for checking
     */
    @Override
    public GitHubPRCause check(@NonNull GitHubPRDecisionContext prDecisionContext) throws IOException {
        TaskListener listener = prDecisionContext.getListener();
        GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();
        GHPullRequest remotePR = prDecisionContext.getRemotePR();
        String fullName = getFullName(prDecisionContext);

        final PrintStream logger = listener.getLogger();

        GitHubPRCause cause = null;

        String pullRequestBody = remotePR.getBody() != null ? remotePR.getBody().trim() : "";
        LOG.debug("Job: '{}', trigger event: '{}', body for test: '{}'",
                fullName,
                DISPLAY_NAME,
                escapeJava(pullRequestBody)
        );

        if (StringUtils.isNotBlank(pullRequestBody)) {
            HashSet<String> skipBuildPhrases = new HashSet<>(Arrays.asList(getSkipMsg().split("[\\r\\n]+")));
            skipBuildPhrases.remove("");
            LOG.debug("Job: '{}', trigger event: '{}', skipBuildPhrases: '{}'",
                    fullName,
                    DISPLAY_NAME,
                    skipBuildPhrases
            );

            for (String skipBuildPhrase : skipBuildPhrases) {
                skipBuildPhrase = skipBuildPhrase.trim();
                Pattern skipBuildPhrasePattern = Pattern.compile(skipBuildPhrase);
                if (skipBuildPhrasePattern.matcher(pullRequestBody).matches()) {
                    LOG.info("Job: '{}', trigger event: '{}', PR body '{}' matches skipBuildPhrase '{}'.",
                            fullName,
                            DISPLAY_NAME,
                            escapeJava(pullRequestBody),
                            skipBuildPhrase
                    );
                    logger.println(DISPLAY_NAME + ": Pull request description contains " + skipBuildPhrase + ", skipping");
                    cause = prDecisionContext.newCause("Pull request description contains " + skipBuildPhrase + ", skipping", true);
                    break;
                } else {
                    LOG.trace("Job: '{}', trigger event: '{}', phrase: '{}' didn't match to '{}'",
                            fullName,
                            DISPLAY_NAME,
                            skipBuildPhrase,
                            escapeJava(pullRequestBody)
                    );
                }
            }
        }

        return cause;
    }

    private String getFullName(GitHubPRDecisionContext context) {
        GitHubPRTrigger prTrigger = context.getTrigger();
        if (nonNull(prTrigger)) {
            return prTrigger.getJob().getFullName();
        }

        return context.getScmSource().getOwner().getFullName();
    }

    public String getSkipMsg() {
        return skipMsg;
    }

    @Symbol("description")
    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
