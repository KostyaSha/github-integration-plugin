package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Assert;
import org.junit.Ignore;
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
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alina Karpovich
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRLabelAddedEventTest {

    private static final String MERGE = "merge";
    private static final String REVIEWED = "reviewed";
    private static final String LOCALLY_TESTED = "locally tested";

    @Mock
    private GHPullRequest remotePr;
    @Mock
    private GitHubPRPullRequest localPR;
    @Mock
    private GitHubPRLabel labels;
    @Mock(lenient = true)
    private GHRepository repository;
    @Mock(lenient = true)
    private GHIssue issue;
    @Mock(lenient = true)
    private GHLabel mergeLabel;
    @Mock(lenient = true)
    private GHLabel reviewedLabel;
    @Mock(lenient = true)
    private GHLabel testLabel;
    @Mock(lenient = true)
    private TaskListener listener;
    @Mock
    private PrintStream logger;
    @Mock
    private GitHubPRTrigger trigger;

    private Set<String> checkedLabels = new HashSet<>(asList(MERGE, REVIEWED, LOCALLY_TESTED));

    /**
     * Case when there is three checked labels and there is one that was added and one that already exists.
     */
    @Test
    public void secondOfThreeLabelsWasAdded() throws IOException {
        Set<String> localLabels = new HashSet<>(Collections.singleton(LOCALLY_TESTED));

        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);

        GitHubPRCause cause = new GitHubPRLabelAddedEvent(labels)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .withLocalPR(localPR)
                        .build()
                );
        assertNull(cause);
    }

    /**
     * Case when there is three checked labels and all of them was already added.
     */
    @Test
    @Ignore
    public void thirdOfThreeLabelsWasAdded() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(LOCALLY_TESTED, MERGE));

        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        causeCreationExpectations();

        GitHubPRCause cause = new GitHubPRLabelAddedEvent(labels)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .withLocalPR(localPR)
                        .build()
                );
        assertThat(cause.getLabels(), equalTo(localLabels));
    }

    /**
     * Case when there is three checked labels and all of them was already added.
     */
    @Test
    public void allLabelsAlreadyExist() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(LOCALLY_TESTED, MERGE, REVIEWED));
        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        GitHubPRLabelAddedEvent instance = new GitHubPRLabelAddedEvent(labels);
        GitHubPRCause cause = instance.check(newGitHubPRDecisionContext()
                .withPrTrigger(trigger)
                .withRemotePR(remotePr)
                .withListener(listener)
                .withLocalPR(localPR)
                .build()
        );
        ;
        assertNull(cause);
    }

    /**
     * Case when there is three checked labels and no one of them was removed.
     */
    @Test
    public void noLabelsWasRemoved() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(MERGE, REVIEWED, LOCALLY_TESTED));
        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        GitHubPRCause cause = new GitHubPRLabelAddedEvent(labels)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .withLocalPR(localPR)
                        .build()
                );
        ;
        assertNull(cause);
    }

    private void commonExpectations(Set<String> localLabels) throws IOException {
        when(labels.getLabelsSet()).thenReturn(checkedLabels);
        when(localPR.getLabels()).thenReturn(localLabels);
        when(remotePr.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePr.getRepository()).thenReturn(repository);
        when(repository.getIssue(anyInt())).thenReturn(issue);
        when(repository.getOwnerName()).thenReturn("ownerName");
        when(listener.getLogger()).thenReturn(logger);
    }

    private void causeCreationExpectations() throws IOException {
        GHUser mockUser = mock(GHUser.class);
        GHCommitPointer mockPointer = mock(GHCommitPointer.class);

        when(remotePr.getUser()).thenReturn(mockUser);
        when(remotePr.getHead()).thenReturn(mockPointer);
        when(remotePr.getBase()).thenReturn(mockPointer);
    }
}
