package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
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
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRCommentEventTest {

    @Mock
    private GHPullRequest remotePr;
    @Mock
    private GitHubPRPullRequest localPR;
    @Mock
    private GitHubPRLabel labels;
    @Mock
    private GHRepository repository;
    @Mock
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
    @Mock
    private GHUser author2;
    @Mock
    private GHIssueComment comment;
    @Mock
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

        assertEquals(cause.getCommentAuthorName(), "commentOwnerName");
        assertEquals(cause.getCommentAuthorEmail(), "commentOwner@email.com");
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
        assertEquals(cause.getCommentAuthorName(), "commentOwnerName");
        assertEquals(cause.getCommentAuthorEmail(), "commentOwner@email.com");
        assertNotEquals(cause.getCommentAuthorName(), "commentOwnerName2");
        assertNotEquals(cause.getCommentAuthorEmail(), "commentOwner2@email.com");
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

        assertEquals(cause.getCommentAuthorName(), "commentOwnerName");
        assertEquals(cause.getCommentAuthorEmail(), "commentOwner@email.com");
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
}
