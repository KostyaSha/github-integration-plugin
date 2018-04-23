package com.github.kostyasha.github.integration.branch.dsl.context.events;


import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent;
import com.github.kostyasha.github.integration.branch.events.impl.commitchecks.GitHubBranchCommitCheck;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;

import java.util.ArrayList;
import java.util.List;

public class GitHubBranchCommitChecksDslContext implements Context {

    private GitHubBranchCommitEvent event = new GitHubBranchCommitEvent();
    private List<GitHubBranchCommitCheck> checks = new ArrayList<>();

    public void commitMessagePattern(Runnable closure) {
        GitHubBranchCommitMessageCheckDslContext commitContext = new GitHubBranchCommitMessageCheckDslContext();
        ContextExtensionPoint.executeInContext(closure, commitContext);

        checks.add(commitContext.getCheck());
    }

    public GitHubBranchCommitEvent getEvent() {
        event.setChecks(checks);
        return event;
    }
}
