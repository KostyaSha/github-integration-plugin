package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static com.github.kostyasha.github.integration.generic.GitHubBranchDecisionContext.newGitHubBranchDecisionContext;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class GitHubBranchRestrictionFilterTest {

    private static final String BRANCH1 = "branch1";
    private static final String BRANCH2 = "branch2";
    private static final String BRANCH3 = "3branch";

    private static final String MASTER = "master";
    private static final String OTHER = "other";

    private static final String PATTERN1 = "\\w+\\d+";
    private static final String PATTERN2 = "\\d+\\w+";

    private GitHubBranchRestrictionFilter filter;

    @Mock
    private GHBranch remoteBranch;

    @Mock
    private GitHubBranchRepository localRepo;

    @Mock
    private GitHubBranch localBranch;

    @Mock
    private GitHubBranchTrigger trigger;

    @Mock
    private TaskListener listener;

    private GitHubBranchCause result;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        filter = new GitHubBranchRestrictionFilter();
    }
    @Test
    public void testBranchNameExclude() throws IOException {
        givenMatchAsExact();
        givenRestrictionsShouldExclude();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasFiltered();

        givenRemoteBranchName(OTHER);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();
    }

    @Test
    public void testBranchNameInclude() throws IOException {
        givenMatchAsExact();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();

        givenRemoteBranchName(OTHER);
        whenApplyBranchFilter();
        thenBranchWasFiltered();
    }

    @Test
    public void testBranchNamePatternExclude() throws IOException {
        givenMatchAsPatterns();
        givenRestrictionsShouldExclude();

        givenRemoteBranchName(BRANCH1);
        whenApplyBranchFilter();
        thenBranchWasFiltered();

        givenRemoteBranchName(BRANCH3);
        whenApplyBranchFilter();
        thenBranchWasFiltered();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();
    }

    @Test
    public void testBranchNamePatternInclude() throws IOException {
        givenMatchAsPatterns();

        givenRemoteBranchName(BRANCH1);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();

        givenRemoteBranchName(BRANCH3);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasFiltered();
    }

    private String createTestInput(String... strings) {
        return String.join(System.lineSeparator(), strings);
    }

    private void givenMatchAsExact() {
        filter.setMatchCriteriaStr(createTestInput(MASTER, BRANCH1, BRANCH2, BRANCH3));
    }

    private void givenMatchAsPatterns() {
        filter.setMatchAsPattern(true);
        filter.setMatchCriteriaStr(createTestInput(PATTERN1, PATTERN2));
    }

    private void givenRemoteBranchName(String name) {
        when(remoteBranch.getName()).thenReturn(name);
    }

    private void givenRestrictionsShouldExclude() {
        filter.setExclude(true);
    }

    private void thenBranchWasFiltered() {
        assertThat("buildable filtered", result.isSkip(), is(true));
    }

    private void thenBranchWasNotFiltered() {
        assertThat("buildable not filtered", result, nullValue());
    }

    private void whenApplyBranchFilter() throws IOException {
        result = filter.check(newGitHubBranchDecisionContext()
                .withLocalBranch(localBranch)
                .withBranchTrigger(trigger)
                .withLocalRepo(localRepo)
                .withRemoteBranch(remoteBranch)
                .withListener(listener)
                .build());
    }
}
