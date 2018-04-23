package com.github.kostyasha.github.integration.branch.dsl.context;

import com.github.kostyasha.github.integration.branch.dsl.context.events.GitHubBranchEventsDslContext;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.dsl.repoproviders.GitHubRepoProvidersDslContext;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.GitHubPRTriggerModeDslContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTriggerDslContext implements Context {
    private String cron = "H/5 * * * *";
    private GitHubPRTriggerMode mode = GitHubPRTriggerMode.CRON;
    private boolean setPreStatus;
    private boolean cancelQueued;
    private boolean abortRunning;

    private List<String> whitelistedBranches = new ArrayList<>();
    private List<GitHubBranchEvent> events = new ArrayList<>();
    private List<GitHubRepoProvider> repoProviders = new ArrayList<>(asList(new GitHubPluginRepoProvider()));


    public void cron(String cron) {
        this.cron = cron;
    }

    public void mode(Runnable closure) {
        GitHubPRTriggerModeDslContext modeContext = new GitHubPRTriggerModeDslContext();
        ContextExtensionPoint.executeInContext(closure, modeContext);

        mode = modeContext.mode();
    }

    public void setPreStatus() {
        setPreStatus = true;
    }

    public void cancelQueued() {
        cancelQueued = true;
    }

    public void abortRunning() {
        abortRunning = true;
    }

    public void events(Runnable closure) {
        GitHubBranchEventsDslContext eventsContext = new GitHubBranchEventsDslContext();
        ContextExtensionPoint.executeInContext(closure, eventsContext);

        events.addAll(eventsContext.events());
    }

    public void repoProviders(Runnable closure) {
        GitHubRepoProvidersDslContext repoProvidersContext = new GitHubRepoProvidersDslContext();
        ContextExtensionPoint.executeInContext(closure, repoProvidersContext);

        repoProviders.clear();
        repoProviders.addAll(repoProvidersContext.repoProviders());
    }

    public void whitelistedBranches(String... branches) {
        whitelistedBranches.addAll(Arrays.asList(branches));
    }

    public String cron() {
        return cron;
    }

    public GitHubPRTriggerMode mode() {
        return mode;
    }

    public boolean isSetPreStatus() {
        return setPreStatus;
    }

    public boolean isCancelQueued() {
        return cancelQueued;
    }

    public boolean isAbortRunning() {
        return abortRunning;
    }

    public List<GitHubBranchEvent> events() {
        return events;
    }

    public List<String> whitelistedBranches() {
        return whitelistedBranches;
    }

    public List<GitHubRepoProvider> repoProviders() {
        return repoProviders;
    }
}
