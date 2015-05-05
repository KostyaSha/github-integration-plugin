package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;

import static org.mockito.Mockito.*;

/**
 * @author Alina Karpovich
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRNonMergeableEventTest {

    @Mock private GitHubPRTrigger trigger;
    @Mock private GHPullRequest remotePr;
    @Mock private GitHubPRPullRequest localPr;
    @Mock private TaskListener listener;
    @Mock private PrintStream logger;

    @Test
    public void remotePRIsMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent();

        when(remotePr.getMergeable()).thenReturn(true);
        when(listener.getLogger()).thenReturn(logger);

        Assert.assertFalse(instance.isSkip(trigger, remotePr, localPr, listener));
    }

    @Test
    public void remotePRIsNotMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent();

        when(remotePr.getMergeable()).thenReturn(false);
        when(listener.getLogger()).thenReturn(logger);

        Assert.assertTrue(instance.isSkip(trigger, remotePr, localPr, listener));
    }

    @Test
    public void remotePRReturnsNullForMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent();

        when(remotePr.getMergeable()).thenReturn(null);
        when(listener.getLogger()).thenReturn(logger);

        Assert.assertTrue(instance.isSkip(trigger, remotePr, localPr, listener));
    }

    @Test
    public void remotePRThrowsExceptionForMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent();

        when(remotePr.getMergeable()).thenThrow(new IOException("test IO"));
        when(listener.getLogger()).thenReturn(logger);

        Assert.assertTrue(instance.isSkip(trigger, remotePr, localPr, listener));
    }
}
