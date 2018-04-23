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
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRNumberTest {
    @Mock
    private GHPullRequest remotePr;
    @Mock
    private GitHubPRPullRequest localPR;
    @Mock
    private TaskListener listener;
    @Mock
    private GHUser mockUser;
    @Mock
    private GHCommitPointer mockPointer;

    @Mock
    private GitHubPRLabel labels;
    @Mock
    private GHRepository repository;
    @Mock
    private GHIssue issue;
    @Mock
    private GHLabel label;
    @Mock
    private PrintStream logger;
    @Mock
    private GitHubPRTrigger trigger;


    @Test
    public void nullNumber() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        final GitHubPRNumber event = new GitHubPRNumber(null, true, true);

        final GitHubPRCause cause = event
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertThat(cause, notNullValue());
        assertThat(cause.isSkip(), is(true));
    }

    @Test
    public void numberNotMatchSkip() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        final GitHubPRNumber event = new GitHubPRNumber(15, false, true);

        final GitHubPRCause cause = event
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .withLocalPR(null)
                        .build()
                );
        assertThat(cause, notNullValue());
        assertThat(cause.isSkip(), is(true));
    }

    @Test
    public void numberMatchSkip() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        when(remotePr.getNumber()).thenReturn(16);

        final GitHubPRNumber event = new GitHubPRNumber(16, true, true);

        final GitHubPRCause cause = event.check(newGitHubPRDecisionContext()
                .withPrTrigger(trigger)
                .withRemotePR(remotePr)
                .withListener(listener)
                .withLocalPR(null)
                .build()
        );
        ;
        assertThat(cause, notNullValue());
        assertThat(cause.isSkip(), is(true));
    }

    @Test
    public void numberMatchTrigger() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        when(remotePr.getNumber()).thenReturn(16);

        final GitHubPRNumber event = new GitHubPRNumber(16, true, false);

        final GitHubPRCause cause = event.check(newGitHubPRDecisionContext()
                .withPrTrigger(trigger)
                .withRemotePR(remotePr)
                .withListener(listener)
                .withLocalPR(null)
                .build()
        );
        assertThat(cause, notNullValue());
        assertThat(cause.isSkip(), is(false));
    }

    @Test
    public void otherNumberMatchTrigger() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        when(remotePr.getNumber()).thenReturn(17);

        final GitHubPRNumber event = new GitHubPRNumber(16, true, false);

        final GitHubPRCause cause = event.check(newGitHubPRDecisionContext()
                .withPrTrigger(trigger)
                .withRemotePR(remotePr)
                .withListener(listener)
                .withLocalPR(null)
                .build()
        );
        assertThat(cause, nullValue());
    }


    private void commonExpectations() throws IOException {
        when(localPR.getLabels()).thenReturn(Collections.<String>emptySet());
        when(remotePr.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePr.getRepository()).thenReturn(repository);
        when(repository.getIssue(anyInt())).thenReturn(issue);
        when(listener.getLogger()).thenReturn(logger);
    }

    private void causeCreationExpectations() throws IOException {
        when(remotePr.getUser()).thenReturn(mockUser);
        when(remotePr.getHead()).thenReturn(mockPointer);
        when(remotePr.getBase()).thenReturn(mockPointer);
    }
}
