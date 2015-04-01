package org.jenkinsci.plugins.github.pullrequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryFactory extends TransientProjectActionFactory {
    private final static Logger LOGGER = Logger.getLogger(GitHubPRRepositoryFactory.class.getName());

    @Override
    public Collection<? extends Action> createFor(AbstractProject project) {
        if (project.getTrigger(GitHubPRTrigger.class) != null) {
            try {
                return Collections.singleton(GitHubPRRepository.forProject(project));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Can't save create GitHubPRRepository Action", e);
            }
        }

        return Collections.emptyList();
    }

}
