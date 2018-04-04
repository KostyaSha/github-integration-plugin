package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class LocalRepoUpdaterTest {
    public static final String SHA_LOCAL = "abc";
    public static final String SHA_REMOTE = "cde";

    @Mock
    private GHPullRequest remotePR;

    @Mock
    private GHRepository remoteRepo;

    @Mock
    private GitHubPRPullRequest localPR;

    @Mock
    private GHCommitPointer commit;

    private GitHubPRRepository localRepo;

    @Before
    public void setUp() throws Exception {
        localRepo = new GitHubPRRepository(remoteRepo);

        when(remotePR.getUser()).thenReturn(new GHUser());
        when(remotePR.getHead()).thenReturn(commit);
        when(remotePR.getBase()).thenReturn(commit);
        when(remotePR.getRepository()).thenReturn(remoteRepo);
        when(remoteRepo.getIssue(Matchers.any(Integer.class))).thenReturn(new GHIssue());

        when(commit.getSha()).thenReturn(SHA_REMOTE);
    }

    @Test
    public void shouldAddOpenedPRs() throws Exception {
        when(remotePR.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePR.getNumber()).thenReturn(1);

        LocalRepoUpdater.updateLocalRepo(localRepo).apply(remotePR);

        assertThat("new", localRepo.getPulls(), hasKey(1));
    }

    @Test
    public void shouldReplaceOpenedPRs() throws Exception {
        when(localPR.getHeadSha()).thenReturn(SHA_LOCAL);
        localRepo.getPulls().put(1, localPR);

        when(remotePR.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePR.getNumber()).thenReturn(1);

        LocalRepoUpdater.updateLocalRepo(localRepo).apply(remotePR);

        assertThat("replace", localRepo.getPulls(), hasKey(1));
        assertThat("sha", localRepo.getPulls().get(1).getHeadSha(), is(SHA_REMOTE));
    }

    @Test
    public void shouldRemoveClosedPRs() throws Exception {
        localRepo.getPulls().put(1, localPR);

        when(remotePR.getState()).thenReturn(GHIssueState.CLOSED);
        when(remotePR.getNumber()).thenReturn(1);

        LocalRepoUpdater.updateLocalRepo(localRepo).apply(remotePR);

        assertThat("replace", localRepo.getPulls(), not(hasKey(1)));
    }
}
