package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.collect.ImmutableMap;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.NotUpdatedPRFilter.notUpdated;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class NotUpdatedPRFilterTest {
    @Mock
    private GitHubPRRepository localRepo;

    @Mock
    private GitHubPRPullRequest localPR;

    @Mock
    private GHPullRequest remotePR;

    @Mock
    private GHCommitPointer commit;

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();

    @Test
    public void shouldBeUpdatedOnNullLocalPR() throws Exception {
        when(localRepo.getPulls()).thenReturn(new HashMap<Integer, GitHubPRPullRequest>());

        assertThat("null localRepo", notUpdated(localRepo, tlRule.getListener()).apply(remotePR), is(true));
    }

    @Test
    public void shouldBeUpdatedOnChangedIssue() throws Exception {
        when(remotePR.getNumber()).thenReturn(1);
        when(localRepo.getPulls()).thenReturn(ImmutableMap.of(1, localPR));

        when(localPR.getIssueUpdatedAt()).thenReturn(new Date(System.currentTimeMillis()));
        when(remotePR.getIssueUpdatedAt()).thenReturn(new Date(System.currentTimeMillis() + 1));
        when(remotePR.getHead()).thenReturn(commit);

        assertThat("issue", notUpdated(localRepo, tlRule.getListener()).apply(remotePR), is(true));
    }

    @Test
    public void shouldBeUpdatedOnChangedPR() throws Exception {
        when(remotePR.getNumber()).thenReturn(1);
        when(localRepo.getPulls()).thenReturn(ImmutableMap.of(1, localPR));

        when(localPR.getPrUpdatedAt()).thenReturn(new Date(System.currentTimeMillis()));
        when(remotePR.getUpdatedAt()).thenReturn(new Date(System.currentTimeMillis() + 1));
        when(remotePR.getHead()).thenReturn(commit);

        assertThat("pr", notUpdated(localRepo, tlRule.getListener()).apply(remotePR), is(true));
    }

    @Test
    public void shouldBeUpdatedOnChangedSha() throws Exception {
        when(remotePR.getNumber()).thenReturn(1);
        when(localRepo.getPulls()).thenReturn(ImmutableMap.of(1, localPR));

        when(localPR.getPrUpdatedAt()).thenReturn(new Date(System.currentTimeMillis()));
        when(remotePR.getUpdatedAt()).thenReturn(new Date(System.currentTimeMillis() + 1));

        when(remotePR.getHead()).thenReturn(commit);
        when(commit.getSha()).thenReturn("def");
        when(localPR.getHeadSha()).thenReturn("abc");

        assertThat("sha", notUpdated(localRepo, tlRule.getListener()).apply(remotePR), is(true));
    }

    @Test
    public void shouldBeNotUpdatedOnSameAsLocal() throws Exception {
        when(remotePR.getNumber()).thenReturn(1);
        when(localRepo.getPulls()).thenReturn(ImmutableMap.of(1, localPR));

        Date date = new Date(System.currentTimeMillis());

        when(localPR.getPrUpdatedAt()).thenReturn(date);
        when(remotePR.getUpdatedAt()).thenReturn(date);

        when(localPR.getIssueUpdatedAt()).thenReturn(date);
        when(remotePR.getIssueUpdatedAt()).thenReturn(date);

        when(remotePR.getHead()).thenReturn(commit);

        when(commit.getSha()).thenReturn("abc");
        when(localPR.getHeadSha()).thenReturn("abc");

        assertThat("not upd", notUpdated(localRepo, tlRule.getListener()).apply(remotePR), is(false));
    }
}
