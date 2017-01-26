package com.github.kostyasha.github.integration.branch.dsl.context.events;


import com.github.kostyasha.github.integration.branch.events.impl.commitchecks.GitHubBranchCommitCheck;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent;

import java.util.ArrayList;
import java.util.List;

import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;

public class GitHubBranchCommitChecksDslContext implements Context {

    private GitHubBranchCommitEvent check = new GitHubBranchCommitEvent();
    private List<GitHubBranchCommitCheck> checks = new ArrayList<>();

    public void commitMessagePattern(Runnable closure) {
        GitHubBranchCommitMessageCheckDslContext commitContext = new GitHubBranchCommitMessageCheckDslContext();
        ContextExtensionPoint.executeInContext(closure, commitContext);

        checks.add(commitContext.getCheck());
    }

    public GitHubBranchCommitEvent getCheck() {
        check.setChecks(checks);
        return check;
    }
}
