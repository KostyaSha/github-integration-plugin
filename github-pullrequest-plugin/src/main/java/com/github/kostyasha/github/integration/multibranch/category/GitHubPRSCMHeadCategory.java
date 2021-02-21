package com.github.kostyasha.github.integration.multibranch.category;

import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.util.NonLocalizable;
import org.jvnet.localizer.Localizable;

import edu.umd.cs.findbugs.annotations.NonNull;

public class GitHubPRSCMHeadCategory extends GitHubSCMHeadCategory {
    public static final GitHubPRSCMHeadCategory PR = new GitHubPRSCMHeadCategory("pr", new NonLocalizable("Pull Requests"));

    public GitHubPRSCMHeadCategory(@NonNull String urlName, Localizable pronoun) {
        super(urlName, pronoun);
    }

    @Override
    public boolean isMatch(@NonNull SCMHead instance) {
        return instance instanceof GitHubPRSCMHead;
    }
}
