package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class SkippedCauseFilterTest {

    @Mock
    private GitHubPRCause cause;

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();

    @Test
    public void shouldSkip() throws Exception {
        when(cause.isSkip()).thenReturn(true);
        assertThat("skip", new SkippedCauseFilter(tlRule.getListener()).apply(cause), is(false));
    }

    @Test
    public void shouldNotSkip() throws Exception {
        when(cause.isSkip()).thenReturn(false);
        assertThat("not skip", new SkippedCauseFilter(tlRule.getListener()).apply(cause), is(true));
    }
}
