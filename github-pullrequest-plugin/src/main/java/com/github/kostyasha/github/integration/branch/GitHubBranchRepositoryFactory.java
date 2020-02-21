package com.github.kostyasha.github.integration.branch;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.generic.GitHubRepositoryFactory;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Create GitHubBranchRepository.
 * Don't depend on remote connection because updateTransientActions() called only once and connection may appear later.
 *
 * @author Kanstantsin Shautsou
 */
@SuppressWarnings("unused")
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
    private static GitHubBranchRepository forProject(@Nonnull Job<?, ?> job) throws IOException {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubBranchRepository.FILE));

        GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
        requireNonNull(trigger, "Can't extract Branch trigger from " + job.getFullName());

        final GitHubRepositoryName repoFullName = trigger.getRepoFullName(job); // ask with job because trigger may not yet be started
        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();

        GitHubBranchRepository localRepository;
        boolean created = false;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubBranchRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, re-creating new one", e);
                localRepository = new GitHubBranchRepository(repoFullName.toString(), new URL(githubUrl));
                created = true;
            }
        } else {
            LOGGER.info("Creating new Branch Repository for '{}'", job.getFullName());
            localRepository = new GitHubBranchRepository(repoFullName.toString(), new URL(githubUrl));
            created = true;
        }

        // set transient cached fields
        localRepository.setJob(job);
        localRepository.setConfigFile(configFile);


        GitHubPRTrigger.DescriptorImpl prTriggerDescriptor = GitHubPRTrigger.DescriptorImpl.get();
        if (prTriggerDescriptor.isActualiseOnFactory()) {
            try {
                localRepository.actualise(trigger.getRemoteRepository(), TaskListener.NULL);
                created = true;
            } catch (Throwable ignore) {
                //silently try actualise
            }
        }

        if (created) localRepository.save();

        return localRepository;
    }

}
