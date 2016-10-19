package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipPRInBadState.badState;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class SkipPRInBadStateTest {
    @Mock
    private GitHubPRRepository localRepo;

    @Mock
    private GitHubPRPullRequest localPR;

    @Mock
    private GHPullRequest remotePR;

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();


    @Test
    public void ignoreNullRemotePR() {
        assertThat(badState(null, null).apply(null),
                is(true));
    }

    @Test
    public void ignoreNullLocalPR() {
        when(remotePR.getNumber()).thenReturn(10);

        Map<Integer, GitHubPRPullRequest> pulls = new HashMap<>();
        pulls.put(10, localPR);
        when(localRepo.getPulls()).thenReturn(pulls);

        assertThat(badState(localRepo, tlRule.getListener()).apply(remotePR),
                is(true));
    }

    @Test
    public void ignoreLocalPRinBadState() {
        when(remotePR.getNumber()).thenReturn(10);
        when(localPR.isInBadState()).thenReturn(true);

        Map<Integer, GitHubPRPullRequest> pulls = new HashMap<>();
        pulls.put(10, localPR);
        when(localRepo.getPulls()).thenReturn(pulls);

        assertThat(badState(localRepo, tlRule.getListener()).apply(remotePR),
                is(false));
    }

    @Test
    public void ignoreLocalPRNotinBadState() {
        when(remotePR.getNumber()).thenReturn(10);
        when(localPR.getLabels()).thenReturn(new HashSet<String>());

        Map<Integer, GitHubPRPullRequest> pulls = new HashMap<>();
        pulls.put(10, localPR);
        when(localRepo.getPulls()).thenReturn(pulls);

        assertThat(badState(localRepo, tlRule.getListener()).apply(remotePR),
                is(true));
    }

}
