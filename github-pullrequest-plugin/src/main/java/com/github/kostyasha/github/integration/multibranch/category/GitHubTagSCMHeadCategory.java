package com.github.kostyasha.github.integration.multibranch.category;

import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.util.NonLocalizable;
import org.jvnet.localizer.Localizable;

import javax.annotation.Nonnull;

public class GitHubTagSCMHeadCategory extends GitHubSCMHeadCategory {
    public static final GitHubTagSCMHeadCategory TAG = new GitHubTagSCMHeadCategory("tag", new NonLocalizable("Tags"));

    public GitHubTagSCMHeadCategory(@Nonnull String name, Localizable displayName) {
        super(name, displayName);
    }

    @Nonnull
    @Override
    protected Localizable defaultDisplayName() {
        return new NonLocalizable("Tag");
    }

    @Override
    public boolean isMatch(@Nonnull SCMHead instance) {
        return instance instanceof GitHubTagSCMHead;
    }
}
