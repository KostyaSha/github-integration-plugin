package com.github.kostyasha.github.integration.multibranch.category;

import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.scm.api.SCMHeadCategory;
import org.jvnet.localizer.Localizable;

import javax.annotation.Nonnull;

public abstract class GitHubSCMHeadCategory extends SCMHeadCategory {

    public GitHubSCMHeadCategory(@Nonnull String urlName, Localizable pronoun) {
        super(urlName, pronoun);
    }
}
