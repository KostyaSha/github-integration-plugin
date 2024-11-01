package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
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
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;

import static com.github.kostyasha.github.integration.generic.GitHubPRDecisionContext.newGitHubPRDecisionContext;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRDescriptionEventTest {
    @Mock
    private ItemGroup parent;

    @Mock
    private GHPullRequest remotePr;

    @Mock(lenient = true)
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

        when(remotePr.getBody()).thenReturn("must skip ci body");

        GitHubPRCause cause = new GitHubPRDescriptionEvent(".*[skip ci].*")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertNotNull(cause);
        assertThat(cause.isSkip(), is(true));
    }

    @Test
    public void skipDescriptionExistNotMatch() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        when(listener.getLogger()).thenReturn(logger);

        when(remotePr.getBody()).thenReturn("unmatched comment");

        GitHubPRCause cause = new GitHubPRDescriptionEvent(".*skip ci.*")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertThat(cause, nullValue());
    }

    @Test
    public void skipDescriptionNotExist() throws IOException {
        commonExpectations();
        causeCreationExpectations();

        when(listener.getLogger()).thenReturn(logger);

        when(remotePr.getBody()).thenReturn(null);

        GitHubPRCause cause = new GitHubPRDescriptionEvent(".*[skip ci].*")
                .check(newGitHubPRDecisionContext()
                        .withPrTrigger(trigger)
                        .withRemotePR(remotePr)
                        .withListener(listener)
                        .build()
                );

        assertThat(cause, nullValue());
    }

    private void commonExpectations() throws IOException {
        when(parent.getFullName()).thenReturn("Full job name");

        FreeStyleProject p = new FreeStyleProject(parent, "p");
        when(trigger.getJob()).thenReturn(p);

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
