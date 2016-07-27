package org.jenkinsci.plugins.github.pullrequest.dsl.context.steps;

import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;

public class GitHubPRStatusStepDslContext implements Context {
    private GitHubPRMessage message;

    public void message(String content) {
        message = new GitHubPRMessage(content);
    }

    public GitHubPRMessage message() {
        return message;
    }
}
