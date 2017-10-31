package com.github.kostyasha.github.integration.multibranch;

import hudson.model.Action;
import jenkins.branch.BranchSource;
import jenkins.branch.MultiBranchProject;
import jenkins.model.TransientActionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

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
//            List<Action> result = new ArrayList<>();
//            for (BranchSource b : project.getSources()) {
//                List<Action> actions = project.getSCMSources());
//                if (actions != null && !actions.isEmpty()) {
//                    result.addAll(actions);
//                }
//            }
//            return result;
        } catch (Exception ex) {
            LOG.warn("Bad configured project {} - {}", project.getFullName(), ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }
}
