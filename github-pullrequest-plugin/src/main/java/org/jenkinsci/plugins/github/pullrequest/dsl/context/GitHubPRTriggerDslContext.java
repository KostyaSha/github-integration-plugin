package org.jenkinsci.plugins.github.pullrequest.dsl.context;

import com.github.kostyasha.github.integration.generic.GitHubRepoProvider;
import com.github.kostyasha.github.integration.generic.dsl.repoproviders.GitHubRepoProvidersDslContext;
import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.events.GitHubPREventsDslContext;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPRTriggerDslContext implements Context {
    private String cron = "H/5 * * * *";
    private GitHubPRTriggerMode mode = GitHubPRTriggerMode.CRON;
    private boolean setPreStatus;
    private boolean cancelQueued;
    private boolean abortRunning;
    private boolean skipFirstRun;
    private List<GitHubPREvent> events = new ArrayList<>();
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

    public void skipFirstRun() {
        skipFirstRun = true;
    }

    public void events(Runnable closure) {
        GitHubPREventsDslContext eventsContext = new GitHubPREventsDslContext();
        ContextExtensionPoint.executeInContext(closure, eventsContext);

        events.addAll(eventsContext.events());
    }

    public void repoProviders(Runnable closure) {
        GitHubRepoProvidersDslContext repoProvidersContext = new GitHubRepoProvidersDslContext();
        ContextExtensionPoint.executeInContext(closure, repoProvidersContext);

        repoProviders.clear();
        repoProviders.addAll(repoProvidersContext.repoProviders());
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

    public boolean isSkipFirstRun() {
        return skipFirstRun;
    }

    public List<GitHubPREvent> events() {
        return events;
    }

    public List<GitHubRepoProvider> repoProviders() {
        return repoProviders;
    }
}
