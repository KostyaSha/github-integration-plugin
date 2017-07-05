package com.github.kostyasha.github.integration.generic.dsl.repoproviders;

import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPluginRepoProviderDslContext implements Context {

    private GitHubPluginRepoProvider provider = new GitHubPluginRepoProvider();

    public void cacheConnection(boolean cacheConnection) {
        provider.setCacheConnection(cacheConnection);
    }

    public void manageHooks(boolean manageHooks) {
        provider.setManageHooks(manageHooks);
    }

    public void permission(Runnable closure) {
        GHRepoPermissionDslContext permissionContext = new GHRepoPermissionDslContext();
        ContextExtensionPoint.executeInContext(closure, permissionContext);

        provider.setRepoPermission(permissionContext.permission());
    }

    public GitHubPluginRepoProvider getProvider() {
        return provider;
    }

}
