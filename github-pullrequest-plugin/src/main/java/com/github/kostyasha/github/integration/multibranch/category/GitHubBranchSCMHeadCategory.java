package com.github.kostyasha.github.integration.multibranch.category;

import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.util.NonLocalizable;
import org.jvnet.localizer.Localizable;

import javax.annotation.Nonnull;

public class GitHubBranchSCMHeadCategory extends GitHubSCMHeadCategory {
    public static final GitHubBranchSCMHeadCategory BRANCH = new GitHubBranchSCMHeadCategory(new NonLocalizable("Branches"));

    public GitHubBranchSCMHeadCategory(@Nonnull String urlName, Localizable pronoun) {
        super(urlName, pronoun);
    }

    public GitHubBranchSCMHeadCategory(Localizable pronoun) {
        super(pronoun);
    }

    @Override
    public boolean isMatch(@Nonnull SCMHead instance) {
        return instance instanceof GitHubBranchSCMHead;
    }
}
