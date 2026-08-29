package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.scheduler.CronTab;
import hudson.scheduler.CronTabList;
import hudson.scheduler.Hash;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRCommentEventTest {
    private static final long CLOSED_PR_COMMENT_GRACE_MILLIS = 10 * 1000L;

    @Mock
    private GHPullRequest remotePr;
    @Mock(lenient = true)
    private GitHubPRPullRequest localPR;
    @Mock(lenient = true)
    private GitHubPRLabel labels;
    @Mock(lenient = true)
    private GHRepository repository;
    @Mock(lenient = true)
    private GHIssue issue;
    @Mock
    private GHLabel mergeLabel;
    @Mock
    private GHLabel reviewedLabel;
    @Mock
    private GHLabel testLabel;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;

    @Mock
    private GitHubPRTrigger trigger;

    @Mock
    private GHUser author;
    @Mock(lenient = true)
    private GHUser author2;
    @Mock
    private GHIssueComment comment;
    @Mock(lenient = true)
    private GHIssueComment comment2;

    @Test
    public void testNullLocalComment() throws IOException {
        when(listener.getLogger()).thenReturn(logger);

        when(issue.getCreatedAt()).thenReturn(new Date());
        when(comment.getBody()).thenReturn("body");

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("Comment")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertNull(cause);
    }

    @Test
    public void testNullLocalCommentRemoteMatch() throws IOException {
        commonExpectations(emptySet());
        causeCreationExpectations();

        final String body = "test foo, bar tags please.";
        when(issue.getCreatedAt()).thenReturn(new Date());
        when(comment.getCreatedAt()).thenReturn(new Date());
        when(comment.getBody()).thenReturn(body);

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("test ([A-Za-z0-9 ,!]+) tags please.")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertThat(cause.getCommentAuthorName(), is("commentOwnerName"));
        assertThat(cause.getCommentAuthorEmail(), is("commentOwner@email.com"));
        assertThat(cause.getCommentBody(), is(body));
        assertThat(cause.getCommentBodyMatch(), is("foo, bar"));
        assertNotNull(cause);
    }

    @Test
    public void firstCommentMatchSecondDont() throws IOException {
        commonExpectations(emptySet());
        causeCreationExpectations();

        when(issue.getCreatedAt()).thenReturn(new Date());

        final String body = "test foo, bar tags please.";
        when(comment.getBody()).thenReturn(body);
        when(comment.getCreatedAt()).thenReturn(new Date());

        final String body2 = "no matching in second comment";
        when(comment2.getUser()).thenReturn(author2);
        when(comment2.getBody()).thenReturn(body2);
        when(comment2.getCreatedAt()).thenReturn(new Date());
        when(author2.getName()).thenReturn("commentOwnerName2");
        when(author2.getEmail()).thenReturn("commentOwner2@email.com");


        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        ghIssueComments.add(comment2);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("test ([A-Za-z0-9 ,!]+) tags please.")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertThat(cause.getCommentAuthorName(), is("commentOwnerName"));
        assertThat(cause.getCommentAuthorEmail(), is("commentOwner@email.com"));
        assertThat(cause.getCommentAuthorName(), not("commentOwnerName2"));
        assertThat(cause.getCommentAuthorEmail(), not("commentOwner2@email.com"));
        assertNotNull(cause);
        assertThat(cause.getCommentBody(), is(body));
        assertThat(cause.getCommentBodyMatch(), is("foo, bar"));
    }

    @Test
    public void testNoComments() throws IOException {
        when(remotePr.getComments()).thenReturn(emptyList());
        when(remotePr.getNumber()).thenReturn(14);
        when(listener.getLogger()).thenReturn(logger);

        GitHubPRCause cause = new GitHubPRCommentEvent("Comment")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertNull(cause);
    }

    @Test
    public void testNullLocalPR() throws IOException {
        commonExpectations(emptySet());
        causeCreationExpectations();

        final String body = "test foo, bar tags please.";
        when(issue.getCreatedAt()).thenReturn(new Date());
        when(comment.getCreatedAt()).thenReturn(new Date());
        when(comment.getBody()).thenReturn(body);

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("test ([A-Za-z0-9 ,!]+) tags please.")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                ); // localPR is null

        assertThat(cause.getCommentAuthorName(), is("commentOwnerName"));
        assertThat(cause.getCommentAuthorEmail(), is("commentOwner@email.com"));
        assertThat(cause.getCommentBody(), is(body));
        assertThat(cause.getCommentBodyMatch(), is("foo, bar"));
        assertNotNull(cause);
    }

    @Test
    public void testClosedPrSkipsHistoricComments() throws IOException {
        commonExpectations(emptySet());
        when(remotePr.getState()).thenReturn(GHIssueState.CLOSED);
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        Date oldCommentDate = new Date(System.currentTimeMillis() - CLOSED_PR_COMMENT_GRACE_MILLIS - 20000L);
        when(comment.getCreatedAt()).thenReturn(oldCommentDate);
        when(comment.getUpdatedAt()).thenReturn(oldCommentDate);
        when(comment.getBody()).thenReturn("test foo, bar tags please.");

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("test ([A-Za-z0-9 ,!]+) tags please.")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                ); // localPR is null and PR is closed

        assertNull(cause);
    }

    @Test
    public void testClosedPrMatchesLatestCommentUpdate() throws IOException {
        commonExpectations(emptySet());
        causeCreationExpectations();
        when(remotePr.getState()).thenReturn(GHIssueState.CLOSED);
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.HEAVY_HOOKS);

        Date commentDate = new Date(System.currentTimeMillis() - 1000L);
        when(comment.getCreatedAt()).thenReturn(new Date(System.currentTimeMillis() - CLOSED_PR_COMMENT_GRACE_MILLIS));
        when(comment.getUpdatedAt()).thenReturn(commentDate);

        final String body = "test foo, bar tags please.";
        when(comment.getBody()).thenReturn(body);

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("test ([A-Za-z0-9 ,!]+) tags please.")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                ); // localPR is null and PR is closed

        assertThat(cause.getCommentAuthorName(), is("commentOwnerName"));
        assertThat(cause.getCommentAuthorEmail(), is("commentOwner@email.com"));
        assertThat(cause.getCommentBody(), is(body));
        assertThat(cause.getCommentBodyMatch(), is("foo, bar"));
        assertNotNull(cause);
    }

    @Test
    public void testClosedPrCronWindowAcceptsRecentComment() throws IOException {
        commonExpectations(emptySet());
        causeCreationExpectations();
        when(remotePr.getState()).thenReturn(GHIssueState.CLOSED);
        when(trigger.getTriggerMode()).thenReturn(GitHubPRTriggerMode.CRON);
        when(trigger.getSpec()).thenReturn("H/5 * * * *");

        Job<?, ?> job = mock(Job.class);
        when(job.getFullName()).thenReturn("pr-comment-cron-window");
        when(trigger.getJob()).thenReturn(job);

        long intervalMillis = computeCronIntervalMillis("H/5 * * * *", "pr-comment-cron-window");
        Date recentCommentDate = new Date(System.currentTimeMillis() - intervalMillis + 2000L);
        when(comment.getCreatedAt()).thenReturn(new Date(System.currentTimeMillis() - intervalMillis - 60000L));
        when(comment.getUpdatedAt()).thenReturn(recentCommentDate);

        final String body = "test foo, bar tags please.";
        when(comment.getBody()).thenReturn(body);

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("test ([A-Za-z0-9 ,!]+) tags please.")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                ); // localPR is null and PR is closed

        assertThat(cause.getCommentAuthorName(), is("commentOwnerName"));
        assertThat(cause.getCommentAuthorEmail(), is("commentOwner@email.com"));
        assertThat(cause.getCommentBody(), is(body));
        assertThat(cause.getCommentBodyMatch(), is("foo, bar"));
        assertNotNull(cause);
    }

    private void commonExpectations(Set<String> localLabels) throws IOException {
        when(labels.getLabelsSet()).thenReturn(localLabels);
        when(localPR.getLabels()).thenReturn(localLabels);
        when(remotePr.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePr.getRepository()).thenReturn(repository);
        when(repository.getIssue(anyInt())).thenReturn(issue);
        when(repository.getOwnerName()).thenReturn("ownerName");
        when(listener.getLogger()).thenReturn(logger);
        when(comment.getUser()).thenReturn(author);
        when(author.getName()).thenReturn("commentOwnerName");
        when(author.getEmail()).thenReturn("commentOwner@email.com");
    }

    private void causeCreationExpectations() throws IOException {
        GHUser mockUser = mock(GHUser.class);
        GHCommitPointer mockPointer = mock(GHCommitPointer.class);
        GHRepository headRepo = mock(GHRepository.class);
        when(headRepo.getOwnerName()).thenReturn("owner");

        when(mockPointer.getRepository()).thenReturn(headRepo);
        when(remotePr.getUser()).thenReturn(mockUser);
        when(remotePr.getHead()).thenReturn(mockPointer);
        when(remotePr.getBase()).thenReturn(mockPointer);
    }

    private long computeCronIntervalMillis(String spec, String seed) {
        List<CronTab> tabs = parseCronTabs(spec, Hash.from(seed));
        Calendar next = nextScheduledAfter(tabs, System.currentTimeMillis() + 1000L);
        Calendar nextAfter = nextScheduledAfter(tabs, next.getTimeInMillis() + 1000L);
        return nextAfter.getTimeInMillis() - next.getTimeInMillis();
    }

    private List<CronTab> parseCronTabs(String spec, Hash hash) {
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
                    throw new IllegalArgumentException("Invalid cron timezone");
                }
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            tabs.add(new CronTab(trimmed, lineNumber, hash, timezone));
        }
        return tabs;
    }

    private Calendar nextScheduledAfter(List<CronTab> tabs, long baseMillis) {
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

    private Calendar calendarFor(CronTab tab, long baseMillis) {
        Calendar calendar = tab.getTimeZone() == null
                ? Calendar.getInstance()
                : Calendar.getInstance(tab.getTimeZone());
        calendar.setTimeInMillis(baseMillis);
        return calendar;
    }
}
