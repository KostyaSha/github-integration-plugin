package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.UserRestrictionFilter.withUserRestriction;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class UserRestrictionFilterTest {
    
    @Mock
    private GitHubPRUserRestriction uRestr;

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();

    @Test
    public void shouldNotFilterWithNotEnabledBranchRestriction() throws Exception {
        assertThat("when br is null", 
                withUserRestriction(tlRule.getListener(), null).apply(new GHPullRequest()), is(true));
    }

    @Test
    public void shouldNotFilterWithNotRestrictedBranchRestriction() throws Exception {
        when(uRestr.isWhitelisted(any(GHUser.class))).thenReturn(true);

        assertThat("when allowed", 
                withUserRestriction(tlRule.getListener(), uRestr).apply(new GHPullRequest()), is(true));
    }

    @Test
    public void shouldFilterWithRestrictedBranchRestriction() throws Exception {
        when(uRestr.isWhitelisted(any(GHUser.class))).thenReturn(false);

        assertThat("when not allowed",
                withUserRestriction(tlRule.getListener(), uRestr).apply(new GHPullRequest()), is(false));
    }
}
