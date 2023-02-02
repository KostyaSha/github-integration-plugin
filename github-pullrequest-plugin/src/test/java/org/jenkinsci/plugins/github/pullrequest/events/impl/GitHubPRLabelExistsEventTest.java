package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Michal Cichra
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRLabelExistsEventTest {

    private static final String MERGE = "merge";
    private static final String REVIEWED = "reviewed";
    private static final String LOCALLY_TESTED = "locally tested";

    @Mock
    private GHPullRequest remotePr;
    @Mock(lenient = true)
    private GitHubPRPullRequest localPR;
    @Mock
    private GitHubPRLabel labels;
    @Mock(lenient = true)
    private GHRepository repository;
    @Mock
    private GHIssue issue;
    @Mock
    private GHLabel mergeLabel;
    @Mock
    private GHLabel reviewedLabel;
    @Mock(lenient = true)
    private GHLabel testLabel;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;
    @Mock
    private GitHubPRTrigger trigger;

    private Set<String> checkedLabels = new HashSet<>(asList(MERGE, REVIEWED, LOCALLY_TESTED));

    /**
     * Case when there is three checked labels and there is one that exists.
     */
    @Test
    public void secondOfThreeLabelsExists() throws IOException {
        Set<String> localLabels = new HashSet<>(Collections.singleton(LOCALLY_TESTED));

        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);

        causeCreationExpectations();

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, false)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertThat(cause.getReason(), equalTo("[locally tested] labels exist"));
    }

    /**
     * Case when there is three checked labels and two of them exist.
     */
    @Test
    public void twoOfThreeLabelsExists() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(LOCALLY_TESTED, MERGE));

        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel, mergeLabel);
        Set<String> remoteLabelSet = new HashSet<>(asList(LOCALLY_TESTED, REVIEWED, MERGE));

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        causeCreationExpectations();

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, false)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertThat(cause.getLabels(), equalTo(remoteLabelSet));
    }

    /**
     * Case when there is three checked labels and all of them was exist.
     */
    @Test
    public void allLabelsExist() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(LOCALLY_TESTED, MERGE, REVIEWED));
        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        causeCreationExpectations();

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, false)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertThat(cause.getLabels(), equalTo(localLabels));
        assertThat(cause.isSkip(), equalTo(false));
    }

    /**
     * Case when there is three checked labels and all of them was exist.
     */
    @Test
    public void allLabelsExistAndSkip() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(LOCALLY_TESTED, MERGE, REVIEWED));
        List<GHLabel> remoteLabels = asList(testLabel, reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        causeCreationExpectations();

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, true)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertThat(cause.getLabels(), equalTo(localLabels));
        assertThat(cause.isSkip(), equalTo(true));
    }


    /**
     * Case when there is is existing label and skip is true.
     */
    @Test
    public void someLabelExistsAndSkip() throws IOException {
        Set<String> localLabels = new HashSet<>(asList(LOCALLY_TESTED, REVIEWED));
        List<GHLabel> remoteLabels = asList(reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, true)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertNull(cause);
    }

    /**
     * Case when there is is existing label and skip is true.
     */
    @Test
    public void noneLabelExists() throws IOException {
        Set<String> localLabels = new HashSet<>(Collections.singleton(LOCALLY_TESTED));
        List<GHLabel> remoteLabels = asList(reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, false)
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withLocalPR(localPR)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );
        assertNull(cause);
    }

    /**
     * Case when there is is existing label and skip is true.
     */
    @Test
    public void noneLabelExistsAndSkip() throws IOException {
        Set<String> localLabels = new HashSet<>(Collections.singleton(LOCALLY_TESTED));
        List<GHLabel> remoteLabels = asList(reviewedLabel, mergeLabel);

        commonExpectations(localLabels);

        when(issue.getLabels()).thenReturn(remoteLabels);
        when(testLabel.getName()).thenReturn(LOCALLY_TESTED);
        when(reviewedLabel.getName()).thenReturn(REVIEWED);
        when(mergeLabel.getName()).thenReturn(MERGE);

        GitHubPRCause cause = new GitHubPRLabelExistsEvent(labels, true)
                .check(newGitHubPRDecisionContext()
                .withPrTrigger(trigger)
                .withLocalPR(localPR)
                .withRemotePR(remotePr)
                .withListener(listener)
                .build()
        );

        assertNull(cause);
    }

    private void commonExpectations(Set<String> localLabels) throws IOException {
        when(labels.getLabelsSet()).thenReturn(localLabels);
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

        GHRepository headRepo = mock(GHRepository.class);
        when(headRepo.getOwnerName()).thenReturn("owner");

        when(mockPointer.getRepository()).thenReturn(headRepo);

        when(remotePr.getUser()).thenReturn(mockUser);
        when(remotePr.getHead()).thenReturn(mockPointer);
        when(remotePr.getBase()).thenReturn(mockPointer);
    }
}
