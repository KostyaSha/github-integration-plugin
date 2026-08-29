package org.jenkinsci.plugins.github.pullrequest.events.impl;

import com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import hudson.scheduler.CronTabList;
import hudson.scheduler.Hash;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Trigger PR based on comment pattern.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRCommentEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Comment matched to pattern";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRCommentEvent.class);
    private static final long CLOSED_PR_COMMENT_GRACE_MILLIS = 10 * 1000L;

    private String comment = "";

    public String getComment() {
        return comment;
    }

    @DataBoundConstructor
    public GitHubPRCommentEvent(String comment) {
        this.comment = comment;
    }

    @Override
    public GitHubPRCause check(@NonNull GitHubPRDecisionContext prDecisionContext) {
        final TaskListener listener = prDecisionContext.getListener();
        final PrintStream llog = listener.getLogger();
        final GitHubPRPullRequest localPR = prDecisionContext.getLocalPR();
        final GHPullRequest remotePR = prDecisionContext.getRemotePR();
        final GitHubPRUserRestriction prUserRestriction = prDecisionContext.getPrUserRestriction();

        GitHubPRCause cause = null;
        final boolean isClosedWithoutLocalState = isNull(localPR)
                && GHIssueState.CLOSED.equals(remotePR.getState());
        final Date closedPrCommentCutoff =
                resolveClosedPrCommentCutoff(prDecisionContext, llog, isClosedWithoutLocalState);
        try {
            for (GHIssueComment issueComment : remotePR.getComments()) {
                if (isClosedWithoutLocalState) {
                    Date commentUpdatedAt = resolveCommentUpdatedAt(issueComment);
                    if (isNull(commentUpdatedAt) || commentUpdatedAt.before(closedPrCommentCutoff)) {
                        continue;
                    }
                }
                if (isNull(localPR) // test all comments for trigger word even if we never saw PR before
                        || isNull(localPR.getLastCommentCreatedAt()) // PR was created but had no comments
                        // don't check comments that we saw before
                        || localPR.getLastCommentCreatedAt().compareTo(issueComment.getCreatedAt()) < 0) {
                    llog.printf("%s: state has changed (new comment found - '%s')%n",
                            DISPLAY_NAME, issueComment.getBody());

                    cause = checkComment(prDecisionContext, issueComment, prUserRestriction, listener);
                    if (nonNull(cause)) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Couldn't obtain comments: {}", e);
            listener.error("Couldn't obtain comments", e);
        }

        if (isNull(cause)) {
            LOG.debug("No matching comments found for {}", remotePR.getNumber());
            llog.println("No matching comments found for " + remotePR.getNumber());
        }

        return cause;
    }

    private Date resolveClosedPrCommentCutoff(GitHubPRDecisionContext prDecisionContext,
                                              PrintStream llog,
                                              boolean shouldResolve) {
        if (!shouldResolve) {
            return null;
        }
        long pollingIntervalMillis = resolvePollingIntervalMillis(prDecisionContext, llog);
        long cutoffMillis = System.currentTimeMillis() - pollingIntervalMillis - CLOSED_PR_COMMENT_GRACE_MILLIS;
        Date cutoff = new Date(cutoffMillis);
        llog.println(DISPLAY_NAME + ": closed PR scan limited to comments updated since " + cutoff);
        return cutoff;
    }

    private long resolvePollingIntervalMillis(GitHubPRDecisionContext prDecisionContext, PrintStream llog) {
        GitHubPRTrigger trigger = prDecisionContext.getTrigger();
        if (isNull(trigger)) {
            return 0L;
        }

        GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();
        if (triggerMode == GitHubPRTriggerMode.HEAVY_HOOKS
                || triggerMode == GitHubPRTriggerMode.LIGHT_HOOKS) {
            return 0L;
        }

        String spec = trigger.getSpec();
        if (isNull(spec) || spec.trim().isEmpty()) {
            llog.println(DISPLAY_NAME + ": empty cron spec, using 0s polling window for closed PR scan");
            return 0L;
        }

        Job<?, ?> job = trigger.getJob();
        String seed = job == null || job.getFullName() == null ? "github-pullrequest-trigger" : job.getFullName();
        List<CronTab> tabs = parseCronTabs(spec, Hash.from(seed), llog);
        if (tabs.isEmpty()) {
            llog.println(DISPLAY_NAME + ": no valid cron entries, using 0s polling window");
            return 0L;
        }
        try {
            Calendar next = nextScheduledAfter(tabs, System.currentTimeMillis() + 1000L);
            if (next == null) {
                llog.println(DISPLAY_NAME + ": unable to resolve cron interval, using 0s polling window");
                return 0L;
            }
            Calendar nextAfter = nextScheduledAfter(tabs, next.getTimeInMillis() + 1000L);
            if (nextAfter == null) {
                llog.println(DISPLAY_NAME + ": unable to resolve cron interval, using 0s polling window");
                return 0L;
            }
            long intervalMillis = nextAfter.getTimeInMillis() - next.getTimeInMillis();
            if (intervalMillis <= 0L) {
                llog.println(DISPLAY_NAME + ": non-positive cron interval, using 0s polling window");
                return 0L;
            }
            return intervalMillis;
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid cron spec '{}' while resolving polling interval for closed PR scan", spec, e);
            llog.println(DISPLAY_NAME + ": invalid cron spec, using 0s polling window");
            return 0L;
        }
    }

    private List<CronTab> parseCronTabs(String spec, Hash hash, PrintStream llog) {
        List<CronTab> tabs = new ArrayList<>();
        String timezone = null;
        int lineNumber = 0;
        for (String line : spec.split("\\r?\\n")) {
            lineNumber++;
            String trimmed = line.trim();
            if (lineNumber == 1 && trimmed.startsWith("TZ=")) {
                String tz = trimmed.replace("TZ=", "");
                timezone = CronTabList.getValidTimezone(tz);
                if (timezone == null) {
                    llog.println(DISPLAY_NAME + ": invalid cron timezone, using 0s polling window");
                    return new ArrayList<>();
                }
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            try {
                tabs.add(new CronTab(trimmed, lineNumber, hash, timezone));
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid cron entry '{}' while resolving polling interval for closed PR scan", trimmed, e);
                llog.println(DISPLAY_NAME + ": invalid cron entry, using 0s polling window");
                return new ArrayList<>();
            }
        }
        return tabs;
    }

    private static Calendar nextScheduledAfter(List<CronTab> tabs, long baseMillis) {
        Calendar next = null;
        for (CronTab tab : tabs) {
            Calendar base = calendarFor(tab, baseMillis);
            Calendar candidate = tab.ceil(base);
            if (candidate == null) {
                continue;
            }
            if (next == null || candidate.before(next)) {
                next = candidate;
            }
        }
        return next;
    }

    private static Calendar calendarFor(CronTab tab, long baseMillis) {
        Calendar calendar = tab.getTimeZone() == null
                ? Calendar.getInstance()
                : Calendar.getInstance(tab.getTimeZone());
        calendar.setTimeInMillis(baseMillis);
        return calendar;
    }

    private static Date resolveCommentUpdatedAt(GHIssueComment issueComment) throws IOException {
        Date updatedAt = issueComment.getUpdatedAt();
        if (nonNull(updatedAt)) {
            return updatedAt;
        }
        return issueComment.getCreatedAt();
    }

    private GitHubPRCause checkComment(GitHubPRDecisionContext prDecisionContext,
                                       GHIssueComment issueComment,
                                       GitHubPRUserRestriction userRestriction,
                                       TaskListener listener) {
        GitHubPRCause cause = null;
        try {
            String body = issueComment.getBody();

            if (isNull(userRestriction) || userRestriction.isWhitelisted(issueComment.getUser())) {
                final Matcher matcher = Pattern.compile(comment).matcher(body);
                if (matcher.matches()) {
                    listener.getLogger().println(DISPLAY_NAME + ": matching comment " + body);
                    LOG.trace("Event matches comment '{}'", body);
                    cause = prDecisionContext.newCause("Comment matches to criteria.", false);
                    cause.withCommentBody(body);
                    cause.withCommentAuthorName(issueComment.getUser().getName());
                    cause.withCommentAuthorEmail(issueComment.getUser().getEmail());
                    if (matcher.groupCount() > 0) {
                        cause.withCommentBodyMatch(matcher.group(1));
                    }
                }
            }
        } catch (IOException ex) {
            LOG.error("Couldn't check comment #{}, skipping it.", issueComment.getId(), ex);
        }
        return cause;
    }

    @Symbol("commentPattern")
    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
