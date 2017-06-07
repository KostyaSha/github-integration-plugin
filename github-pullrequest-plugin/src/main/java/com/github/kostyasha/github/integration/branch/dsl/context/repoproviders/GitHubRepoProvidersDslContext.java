package com.github.kostyasha.github.integration.branch.dsl.context.repoproviders;

import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubRepoProvidersDslContext implements Context {

    private List<GitHubRepoProvider> repoProviders = new ArrayList<>();

    public void gitHubPlugin(Runnable closure) {
        GitHubPluginRepoProviderDslContext githubPluginContext = new GitHubPluginRepoProviderDslContext();
        ContextExtensionPoint.executeInContext(closure, githubPluginContext);

        repoProviders.add(githubPluginContext.getProvider());
    }

    public List<GitHubRepoProvider> repoProviders() {
        return repoProviders;
    }
}
