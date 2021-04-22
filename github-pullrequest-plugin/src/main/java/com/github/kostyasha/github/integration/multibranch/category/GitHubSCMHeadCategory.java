package com.github.kostyasha.github.integration.multibranch.category;

import jenkins.scm.api.SCMHeadCategory;
import org.jvnet.localizer.Localizable;

import edu.umd.cs.findbugs.annotations.NonNull;

public abstract class GitHubSCMHeadCategory extends SCMHeadCategory {

    public GitHubSCMHeadCategory(@NonNull String urlName, Localizable pronoun) {
        super(urlName, pronoun);
    }


    public GitHubSCMHeadCategory(Localizable pronoun) {
        super(pronoun);
    }
}
