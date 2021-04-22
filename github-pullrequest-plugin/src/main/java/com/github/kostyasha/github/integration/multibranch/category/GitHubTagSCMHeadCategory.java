package com.github.kostyasha.github.integration.multibranch.category;

import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.util.NonLocalizable;
import org.jvnet.localizer.Localizable;

import edu.umd.cs.findbugs.annotations.NonNull;

public class GitHubTagSCMHeadCategory extends GitHubSCMHeadCategory {
    public static final GitHubTagSCMHeadCategory TAG = new GitHubTagSCMHeadCategory("tag", new NonLocalizable("Tags"));

    public GitHubTagSCMHeadCategory(@NonNull String name, Localizable displayName) {
        super(name, displayName);
    }

    @NonNull
    @Override
    protected Localizable defaultDisplayName() {
        return new NonLocalizable("Tag");
    }

    @Override
    public boolean isMatch(@NonNull SCMHead instance) {
        return instance instanceof GitHubTagSCMHead;
    }
}
