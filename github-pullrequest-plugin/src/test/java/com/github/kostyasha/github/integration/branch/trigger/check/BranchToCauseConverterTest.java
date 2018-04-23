package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;

import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.github.kostyasha.github.integration.branch.trigger.check.BranchToCauseConverter.toGitHubBranchCause;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BranchToCauseConverterTest {

    @Mock
    private GHBranch mockRemoteBranch;

    @Mock
    private GitHubBranchCause mockCause;

    private List<GitHubBranchEvent> mockEvents;

    @Mock
    private GitHubBranchRepository mockLocalRepo;

    @Mock
    private GitHubBranchTrigger mockTrigger;

    private GitHubBranchCause result;

    @Rule
    public TaskListenerWrapperRule tRule = new TaskListenerWrapperRule();

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testErrorCheckingEvent() throws Exception {
        givenAnEventCheckException();
        whenMapToCause();
        thenNoCauseIsFound();
    }

    @Test
    public void testEventsMatches() throws Exception {
        givenAnEventMatches();
        whenMapToCause();
        thenACausesIsFound();
    }

    @Test
    public void testEventsMatchesButCauseSkipped() throws Exception {
        givenCauseShouldBeSkipped();
        givenAnEventMatches();
        whenMapToCause();
        thenNoCauseIsFound();
    }

    @Test
    public void testNoEventsMatch() throws Exception {
        givenNoEventsMatch();
        whenMapToCause();
        thenNoCauseIsFound();
    }

    private GitHubBranchEvent createExceptionEvent() throws IOException {
        GitHubBranchEvent event = mock(GitHubBranchEvent.class);
        doThrow(IOException.class).when(event).check(any());

        return event;
    }

    private GitHubBranchEvent createMatchingEvent() throws IOException {
        GitHubBranchEvent event = mock(GitHubBranchEvent.class);
        when(event.check(any())).thenReturn(mockCause);

        return event;
    }

    private GitHubBranchEvent createNonMatchingEvent() throws IOException {
        GitHubBranchEvent event = mock(GitHubBranchEvent.class);
        when(event.check(any())).thenReturn(null);

        return event;
    }

    private void givenAnEventCheckException() throws IOException {
        mockEvents = Arrays.asList(createNonMatchingEvent(), createExceptionEvent());
    }

    private void givenAnEventMatches() throws IOException {
        mockEvents = Arrays.asList(createNonMatchingEvent(), createMatchingEvent());
    }

    private void givenCauseShouldBeSkipped() {
        when(mockCause.isSkip()).thenReturn(true);
    }

    private void givenNoEventsMatch() throws IOException {
        mockEvents = Arrays.asList(createNonMatchingEvent(), createNonMatchingEvent());
    }

    private void thenACausesIsFound() {
        assertNotNull(result);
    }

    private void thenNoCauseIsFound() {
        assertNull(result);
    }

    private void whenMapToCause() {
        when(mockTrigger.getEvents()).thenReturn(mockEvents);
        result = toGitHubBranchCause(mockLocalRepo, tRule.getListener(), mockTrigger).apply(mockRemoteBranch);
    }
}
