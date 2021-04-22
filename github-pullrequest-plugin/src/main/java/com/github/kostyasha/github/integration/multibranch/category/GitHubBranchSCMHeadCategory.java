package com.github.kostyasha.github.integration.multibranch.category;

import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import jenkins.scm.api.SCMHead;
import jenkins.util.NonLocalizable;
import org.jvnet.localizer.Localizable;

import edu.umd.cs.findbugs.annotations.NonNull;

public class GitHubBranchSCMHeadCategory extends GitHubSCMHeadCategory {
    public static final GitHubBranchSCMHeadCategory BRANCH = new GitHubBranchSCMHeadCategory(new NonLocalizable("Branches"));

    public GitHubBranchSCMHeadCategory(@NonNull String urlName, Localizable pronoun) {
        super(urlName, pronoun);
    }

    public GitHubBranchSCMHeadCategory(Localizable pronoun) {
        super(pronoun);
    }

    @Override
    public boolean isMatch(@NonNull SCMHead instance) {
        return instance instanceof GitHubBranchSCMHead;
    }
}
