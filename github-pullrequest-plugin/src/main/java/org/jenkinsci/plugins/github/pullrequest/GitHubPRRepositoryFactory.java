package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.generic.GitHubRepositoryFactory;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singleton;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;

/**
 * Create GitHubPRRepository.
 * Don't depend on remote connection because updateTransientActions() called only once and connection may appear later.
 *
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryFactory extends GitHubRepositoryFactory<GitHubPRRepositoryFactory, GitHubPRTrigger> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRepositoryFactory.class);

    @Override
    @NonNull
    public Collection<? extends Action> createFor(@NonNull Job job) {
        try {
            if (nonNull(ghPRTriggerFromJob(job))) {
                return singleton(forProject(job));
            }
        } catch (Exception ex) {
            LOGGER.error("Bad configured project {} - {}", job.getFullName(), ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }

    @NonNull
    private static GitHubPRRepository forProject(Job<?, ?> job) throws IOException {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubPRRepository.FILE));

        GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
        requireNonNull(trigger, "Can't extract PR trigger from " + job.getFullName());

        final GitHubRepositoryName repoFullName = trigger.getRepoFullName(job); // ask with job because trigger may not yet be started
        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();

        boolean save = false;
        GitHubPRRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubPRRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, re-creating new one", e);
                localRepository = new GitHubPRRepository(repoFullName.toString(), new URL(githubUrl));
                save = true;
            }
        } else {
            localRepository = new GitHubPRRepository(repoFullName.toString(), new URL(githubUrl));
            save = true;
        }

        localRepository.setJob(job);
        localRepository.setConfigFile(configFile);

        GitHubPRTrigger.DescriptorImpl prTriggerDescriptor = GitHubPRTrigger.DescriptorImpl.get();
        if (prTriggerDescriptor.isActualiseOnFactory()) {
            try {
                localRepository.actualise(trigger.getRemoteRepository(), TaskListener.NULL);
                save = true;
            } catch (Throwable ignore) {
                //silently try actualise
            }
        }

        if (save) localRepository.save();

        return localRepository;
    }

    @Override
    public Class<Job> type() {
        return Job.class;
    }
}
