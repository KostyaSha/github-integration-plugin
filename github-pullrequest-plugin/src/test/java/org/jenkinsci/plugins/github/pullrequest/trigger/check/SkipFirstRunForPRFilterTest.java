package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHPullRequest;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipFirstRunForPRFilter.ifSkippedFirstRun;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class SkipFirstRunForPRFilterTest {

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();

    @Test
    public void shouldSkipIfFR() throws Exception {
        assertThat("skip", ifSkippedFirstRun(tlRule.getListener(), true).apply(new GHPullRequest()), is(false));
    }

    @Test
    public void shouldNotSkipIfNotSkipFR() throws Exception {
        assertThat("not skip", ifSkippedFirstRun(tlRule.getListener(), false).apply(new GHPullRequest()), is(true));
    }
}
