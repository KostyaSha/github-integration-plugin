package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHPullRequest;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.BranchRestrictionFilter.withBranchRestriction;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class BranchRestrictionFilterTest {
    
    @Mock
    private GitHubPRBranchRestriction bRestr;

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();

    @Test
    public void shouldNotFilterWithNotEnabledBranchRestriction() throws Exception {
        assertThat("when br is null", withBranchRestriction(tlRule.getListener(), null).apply(null), is(true));
    }

    @Ignore("TODO needs triage")
    @Test
    public void shouldNotFilterWithNotRestrictedBranchRestriction() throws Exception {
        when(bRestr.isBranchBuildAllowed(any(GHPullRequest.class))).thenReturn(true);
        
        assertThat("when allowed", withBranchRestriction(tlRule.getListener(), bRestr).apply(null), is(true));
    }

    @Test
    public void shouldFilterWithRestrictedBranchRestriction() throws Exception {
        when(bRestr.isBranchBuildAllowed(any(GHPullRequest.class))).thenReturn(false);
        
        assertThat("when not allowed", 
                withBranchRestriction(tlRule.getListener(), bRestr).apply(new GHPullRequest()), is(false));
    }
}
