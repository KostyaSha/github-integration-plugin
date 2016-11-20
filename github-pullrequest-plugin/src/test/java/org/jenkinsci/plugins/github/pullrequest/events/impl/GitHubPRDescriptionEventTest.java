package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@PrepareForTest(Job.class)
@RunWith(PowerMockRunner.class)
public class GitHubPRDescriptionEventTest {
    @Mock
    private FreeStyleProject job;

    @Mock
    private GHPullRequest remotePr;

    @Mock
    private GHRepository repository;
    @Mock
    private GHIssue issue;

    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;

    @Mock
    private GitHubPRTrigger trigger;

    @Test
    public void skipDescriptionExist() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        when(listener.getLogger()).thenReturn(logger);

        when(issue.getCreatedAt()).thenReturn(new Date());
        when(remotePr.getBody()).thenReturn("must skip ci body");

        GitHubPRCause cause = new GitHubPRDescriptionEvent(".*[skip ci].*")
                .check(trigger, remotePr, null, listener);

        assertNotNull(cause);
        assertThat(cause.isSkip(), is(true));
    }

    private void commonExpectations() throws IOException {
        when(job.getFullName()).thenReturn("Full job name");

        when(trigger.getJob()).thenReturn((Job) job);

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
