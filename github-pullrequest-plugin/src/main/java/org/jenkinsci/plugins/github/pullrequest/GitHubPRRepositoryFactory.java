package org.jenkinsci.plugins.github.pullrequest;

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

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryFactory extends GitHubRepositoryFactory<GitHubPRRepositoryFactory, GitHubPRTrigger> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRepositoryFactory.class);

    @Override
    @Nonnull
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        try {
            if (nonNull(ghPRTriggerFromJob(job))) {
                return singleton(forProject(job));
            }
        } catch (Exception ex) {
            LOGGER.error("Bad configured project {} - {}", job.getFullName(), ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubPRRepository forProject(Job<?, ?> job) throws IOException {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubPRRepository.FILE));

        GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
        requireNonNull(trigger, "Can't extract PR trigger from " + job.getFullName());

        GitHubPRRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubPRRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, creating new one", e);
                final GHRepository ghRepository = trigger.getRemoteRepository();
                requireNonNull(ghRepository, "Can't get remote GH repository.");
                localRepository = new GitHubPRRepository(ghRepository);
            }
        } else {
            final GHRepository ghRepository = trigger.getRemoteRepository();
            requireNonNull(ghRepository, "Can't get remote GH repository.");
            localRepository = new GitHubPRRepository(ghRepository);
        }

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

    @Override
    public Class<Job> type() {
        return Job.class;
    }
}
