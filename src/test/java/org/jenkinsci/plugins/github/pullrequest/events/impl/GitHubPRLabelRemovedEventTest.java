package org.jenkinsci.plugins.github.pullrequest.events.impl;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Alina Karpovich 
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRLabelRemovedEventTest {
    
    private static final String NOT_READY_FOR_MERGE = "not ready for merge";
    private static final String NOT_REVIEWED = "not reviewed";
    private static final String TESTS_FAILURE = "tests failure";
    
    @Mock private GHPullRequest remotePr;
    @Mock private GitHubPRPullRequest localPR;
    @Mock private GitHubPRLabel labels;
    @Mock private GHRepository repository;
    @Mock private GHIssue issue;
    @Mock private GHLabel label;
    
    private Set<String> checkedLabels = new HashSet<String>();
    {
        checkedLabels.add(NOT_READY_FOR_MERGE);
        checkedLabels.add(NOT_REVIEWED);
        checkedLabels.add(TESTS_FAILURE);
    }
    
    /**
     * Case when there is three checked labels and there is one that wasn't removed yet.
     */
    @Test
    public void twoOfThreeLabelsWasRemoved() throws IOException {
        Set<String> localLabels = new HashSet<>();
        localLabels.add(TESTS_FAILURE);   
        
        List<GHLabel> remoteLabels = new ArrayList<GHLabel>();
        remoteLabels.add(label);

        commonExpectations(localLabels);
        when(issue.getLabels()).thenReturn(remoteLabels);
        when(label.getName()).thenReturn(TESTS_FAILURE);
        
        GitHubPRLabelRemovedEvent instance = new GitHubPRLabelRemovedEvent(labels);
        GitHubPRCause cause = instance.isStateChanged(null, remotePr,localPR);
        Assert.assertNull(cause);
    }

    /**
     * Case when there is three checked labels and all of them was already removed.
     */
    @Test
    public void threeOfThreeLabelsWasRemoved() throws IOException {
        Set<String> localLabels = new HashSet<>();
        localLabels.add(TESTS_FAILURE);

        commonExpectations(localLabels);
        when(issue.getLabels())
                .thenReturn(new ArrayList<GHLabel>());
        causeCreationExpectations();

        GitHubPRLabelRemovedEvent instance = new GitHubPRLabelRemovedEvent(labels);
        GitHubPRCause cause = instance.isStateChanged(null, remotePr,localPR);
        Assert.assertEquals("[tests failure, not reviewed, not ready for merge] labels were removed", cause.getReason());
    }

    /**
     * Case when there is three checked labels and no one of them was removed.
     */
    @Test
    public void noLabelsWasRemoved() throws IOException {
        Set<String> localLabels = new HashSet<>();
        localLabels.add(NOT_READY_FOR_MERGE);
        localLabels.add(NOT_REVIEWED);
        localLabels.add(TESTS_FAILURE);

        List<GHLabel> remoteLabels = new ArrayList<GHLabel>();
        for (int i=0; i<localLabels.size(); i++) {
            remoteLabels.add(label);
        }

        commonExpectations(localLabels);
        when(issue.getLabels()).thenReturn(remoteLabels);
        when(label.getName()).thenReturn(TESTS_FAILURE);
        when(label.getName()).thenReturn(NOT_READY_FOR_MERGE);
        when(label.getName()).thenReturn(NOT_REVIEWED);

        GitHubPRLabelRemovedEvent instance = new GitHubPRLabelRemovedEvent(labels);
        GitHubPRCause cause = instance.isStateChanged(null, remotePr,localPR);
        Assert.assertNull(cause);
    }
    
    private void commonExpectations(Set<String> localLabels) throws IOException {
        when(labels.getLabelsSet()).thenReturn(checkedLabels);
        when(localPR.getLabels()).thenReturn(localLabels);
        when(remotePr.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePr.getRepository()).thenReturn(repository);
        when(repository.getIssue(anyInt())).thenReturn(issue);
    }
    
    private void causeCreationExpectations() throws IOException {
        GHUser mockUser = mock(GHUser.class);
        GHCommitPointer mockPointer = mock(GHCommitPointer.class);
        
        when(remotePr.getUser()).thenReturn(mockUser);
        when(remotePr.getHead()).thenReturn(mockPointer);
        when(remotePr.getBase()).thenReturn(mockPointer);
    }
}
