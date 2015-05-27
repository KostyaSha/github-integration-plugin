package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryFactory extends TransientProjectActionFactory {
    private static final Logger LOGGER = Logger.getLogger(GitHubPRRepositoryFactory.class.getName());

    @Override
    public Collection<? extends Action> createFor(AbstractProject project) {
        try {
            if (project.getTrigger(GitHubPRTrigger.class) != null) {
                return Collections.singleton(forProject(project));
            }
        } catch (Throwable t) {
            // bad configured project i.e. github project property wrong
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubPRRepository forProject(AbstractProject<?, ?> job) {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubPRRepository.FILE));

        GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
        String repoFullName = trigger.getRepoFullName(job);

        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();
        GitHubPRRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubPRRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "Can't read saved repository, creating new one", e);
                localRepository = new GitHubPRRepository(repoFullName, githubUrl, new HashMap<Integer, GitHubPRPullRequest>());
            }
        } else {
            localRepository = new GitHubPRRepository(repoFullName, githubUrl, new HashMap<Integer, GitHubPRPullRequest>());
        }

        localRepository.setProject(job);
        localRepository.setConfigFile(configFile);
        return localRepository;
    }

}
