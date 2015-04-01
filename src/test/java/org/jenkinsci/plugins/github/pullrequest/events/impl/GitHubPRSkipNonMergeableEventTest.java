package org.jenkinsci.plugins.github.pullrequest.events.impl;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * @author Alina Karpovich
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRSkipNonMergeableEventTest {

    @Mock private GitHubPRTrigger trigger;
    @Mock private GHPullRequest remotePr;
    @Mock private GitHubPRPullRequest localPr;

    @Test
    public void remotePRIsMergeable() throws IOException {
        GitHubPRSkipNonMergeableEvent instance = new GitHubPRSkipNonMergeableEvent();

        when(remotePr.getMergeable()).thenReturn(true);

        Assert.assertFalse(instance.isSkip(trigger, remotePr, localPr));
    }

    @Test
    public void remotePRIsNotMergeable() throws IOException {
        GitHubPRSkipNonMergeableEvent instance = new GitHubPRSkipNonMergeableEvent();

        when(remotePr.getMergeable()).thenReturn(false);

        Assert.assertTrue(instance.isSkip(trigger, remotePr, localPr));
    }

    @Test
    public void remotePRReturnsNullForMergeable() throws IOException {
        GitHubPRSkipNonMergeableEvent instance = new GitHubPRSkipNonMergeableEvent();

        when(remotePr.getMergeable()).thenReturn(null);

        Assert.assertTrue(instance.isSkip(trigger, remotePr, localPr));
    }

    @Test
    public void remotePRThrowsExceptionForMergeable() throws IOException {
        GitHubPRSkipNonMergeableEvent instance = new GitHubPRSkipNonMergeableEvent();

        when(remotePr.getMergeable()).thenThrow(new IOException("test IO"));

        Assert.assertTrue(instance.isSkip(trigger, remotePr, localPr));
    }
}
