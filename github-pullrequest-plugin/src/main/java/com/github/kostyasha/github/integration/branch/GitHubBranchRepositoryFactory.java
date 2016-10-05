package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubRepositoryFactory;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubBranchRepositoryFactory
        extends GitHubRepositoryFactory<GitHubBranchRepositoryFactory, GitHubBranchTrigger> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchRepositoryFactory.class);

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        try {
            if (nonNull(ghBranchTriggerFromJob(job))) {
                return Collections.singleton(forProject(job));
            }
        } catch (Exception ex) {
            LOGGER.warn("Bad configured project {} - {}", job.getFullName(), ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubBranchRepository forProject(Job<?, ?> job) throws IOException {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubBranchRepository.FILE));

        GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);

        GitHubBranchRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubBranchRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, creating new one", e);
                final GHRepository remoteRepository = trigger.getRemoteRepository();
                localRepository = new GitHubBranchRepository(remoteRepository);
            }
        } else {
            final GHRepository remoteRepository = trigger.getRemoteRepository();
            localRepository = new GitHubBranchRepository(remoteRepository);
        }

        // set transient cached fields
        localRepository.setJob(job);
        localRepository.setConfigFile(configFile);

        if (isNull(localRepository.getGitUrl()) ||
                isNull(localRepository.getSshUrl())) {
            final GHRepository remoteRepository = trigger.getRemoteRepository();
            localRepository.withGitUrl(remoteRepository.getGitTransportUrl())
                    .withSshUrl(remoteRepository.getSshUrl());
            localRepository.save();
        }

        return localRepository;
    }

}
