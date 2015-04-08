package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.*;
import hudson.model.*;
import hudson.model.listeners.SaveableListener;
import hudson.util.RunList;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GitHub Repository local state = last trigger run() state.
 * Store only necessary variables.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRRepository extends AbstractDescribableImpl<GitHubPRRepository> implements Action, Saveable {
    /**
     * Store constantly changing information in project directory with .runtime.xml tail
     */
    public static final String FILE = GitHubPRRepository.class.getName() + ".runtime.xml";
    private static final Logger LOGGER = Logger.getLogger(GitHubPRRepository.class.getName());
    private transient XmlFile configFile; // for save()
    private transient AbstractProject<?, ?> project;  // for UI

    private final String fullName;
    private final String githubUrl;

    private final HashMap<Integer, GitHubPRPullRequest> pulls;

    /**
     * Object that represent GitHub repository to work with
     *
     * @param fullName repository full name. for case of changed jobs url
     * @param pulls    previous pull request state
     */
    public GitHubPRRepository(String fullName, String githubUrl, HashMap<Integer, GitHubPRPullRequest> pulls) {
        this.pulls = pulls;
        this.fullName = fullName;
        this.githubUrl = githubUrl;
    }

    public HashMap<Integer, GitHubPRPullRequest> getPulls() {
        return pulls;
    }

    /**
     * Searches for all builds performed in the runs of current job.
     *
     * @return map with keys - numbers of built PRs and values - lists of related builds.
     */
    public Map<Integer, List<AbstractBuild<?, ?>>> getAllPrBuilds() {

        Map<Integer, List<AbstractBuild<?, ?>>> map = new HashMap<Integer, List<AbstractBuild<?, ?>>>();
        final RunList<? extends AbstractBuild<?, ?>> builds = project.getBuilds();
        LOGGER.log(Level.FINE, "Got {0} builds for project {1}", new Object[]{builds.size(), project.getFullName()});

        for (AbstractBuild build : builds) {
            GitHubPRCause cause = (GitHubPRCause) build.getCause(GitHubPRCause.class);
            if (cause != null) {
                int number = cause.getNumber();
                List<AbstractBuild<?, ?>> buildsByNumber = map.get(number);
                if (buildsByNumber == null) {
                    buildsByNumber = new ArrayList<AbstractBuild<?, ?>>();
                    map.put(number, buildsByNumber);
                }
                buildsByNumber.add(build);
            }
        }

        return map;
    }

    //TODO move to transient factory
    public static GitHubPRRepository forProject(AbstractProject<?, ?> job) throws IOException {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), FILE));

        GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
        String repoFullName = trigger.getRepoFullName(job);

        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();
        GitHubPRRepository localRepository;
        if (configFile.exists()) {
            GitHubPRRepository rep = (GitHubPRRepository) configFile.read();
            if (rep != null) {
                localRepository = rep;
            } else { // loaded bad data
                localRepository = new GitHubPRRepository(repoFullName, githubUrl, new HashMap<Integer, GitHubPRPullRequest>());
            }
        } else {
            localRepository = new GitHubPRRepository(repoFullName, githubUrl, new HashMap<Integer, GitHubPRPullRequest>());
        }

        localRepository.project = job;
        localRepository.configFile = configFile;
        return localRepository;
    }

    public String getFullName() {
        return fullName;
    }

    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-pull-request.svg";
    }

    public String getDisplayName() {
        return "GitHub PR";
    }

    public String getUrlName() {
        return "github-pullrequest";
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public synchronized void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }

        configFile.write(this);
        SaveableListener.fireOnChange(this, configFile);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRRepository> {
        @Override
        public String getDisplayName() {
            return "GitHub PR local repository";
        }
    }
}
