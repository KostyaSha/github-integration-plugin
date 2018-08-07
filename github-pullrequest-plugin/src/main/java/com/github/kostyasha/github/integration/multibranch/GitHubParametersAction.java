package com.github.kostyasha.github.integration.multibranch;

import java.io.IOException;
import java.util.List;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.EnvironmentContributor;
import hudson.model.InvisibleAction;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Queue.Item;
import hudson.model.Queue.Task;
import hudson.model.queue.FoldableAction;

/**
 * Simpler replacement for {@link ParametersAction} that does not stand in the way of 'folding' queue items
 * @author atanasenko
 *
 */
public class GitHubParametersAction extends InvisibleAction implements FoldableAction {

    private List<ParameterValue> params;

    public GitHubParametersAction(List<ParameterValue> params) {
        this.params = params;
    }

    @Override
    public void foldIntoExisting(Item item, Task owner, List<Action> actions) {
        item.addOrReplaceAction(this);
    }

    @Extension
    public static class GitHubEnvContributor extends EnvironmentContributor {
        @Override
        public void buildEnvironmentFor(Run r, EnvVars envs, TaskListener listener) throws IOException, InterruptedException {
            GitHubParametersAction action = r.getAction(GitHubParametersAction.class);
            if (action == null) {
                return;
            }
            action.params.forEach(p -> p.buildEnvironment(r, envs));
        }
    }

}
