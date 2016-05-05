package org.jenkinsci.plugins.github_integration.branch;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.asFullRepoName;
import static org.jenkinsci.plugins.github_integration.branch.utils.JobHelper.ghBranchTriggerFromJob;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubBranchRepositoryFactory extends TransientActionFactory<Job> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchRepositoryFactory.class);

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        try {
            if (nonNull(ghBranchTriggerFromJob(job))) {
                return Collections.singleton(forProject(job));
            }
        } catch (Exception ex) {
            LOGGER.warn("Bad configured project {} - {}", job.getFullName(), ex.getMessage());
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubBranchRepository forProject(Job<?, ?> job) {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubBranchRepository.FILE));

        GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
        GitHubRepositoryName repo = trigger.getRepoFullName(job);

        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();
        GitHubBranchRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubBranchRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, creating new one", e);
                localRepository = new GitHubBranchRepository(asFullRepoName(repo), githubUrl,
                        new HashMap<String, GitHubLocalBranch>());
            }
        } else {
            localRepository = new GitHubBranchRepository(asFullRepoName(repo), githubUrl,
                    new HashMap<String, GitHubLocalBranch>());
        }

        localRepository.setJob(job);
        localRepository.setConfigFile(configFile);
        return localRepository;
    }

    @Override
    public Class<Job> type() {
        return Job.class;
    }
}
