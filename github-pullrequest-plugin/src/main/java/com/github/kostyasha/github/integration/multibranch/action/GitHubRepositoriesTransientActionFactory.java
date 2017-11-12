package com.github.kostyasha.github.integration.multibranch.action;

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import hudson.Extension;
import hudson.model.Action;
import jenkins.branch.MultiBranchProject;
import jenkins.model.TransientActionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@Extension
public class GitHubRepositoriesTransientActionFactory extends TransientActionFactory<MultiBranchProject<?, ?>> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepositoriesTransientActionFactory.class);


    @Override
    public Class type() {
        return MultiBranchProject.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull MultiBranchProject<?, ?> project) {
        try {
            if (project.getSCMSources().stream()
                    .anyMatch(scm -> scm instanceof GitHubSCMSource)) {
                return Collections.singletonList(createActionFor(project));
            }
        } catch (Exception ex) {
            LOG.warn("Bad configured project {} - {}", project.getFullName(), ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }

    @Nonnull
    private Action createActionFor(MultiBranchProject project) throws IOException {

        GitHubSCMSourcesReposAction storageAction = new GitHubSCMSourcesReposAction(project);
        storageAction.load();
        return storageAction;
    }


}
