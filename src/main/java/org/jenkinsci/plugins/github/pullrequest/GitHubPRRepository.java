package org.jenkinsci.plugins.github.pullrequest;

import hudson.*;
import hudson.model.*;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GitHub Repository local state = last trigger run() state.
 * Store only necessary variables.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRRepository implements Action, Saveable {
    /**
     * Store constantly changing information in project directory with .runtime.xml tail
     */
    public static final String FILE = GitHubPRRepository.class.getName() + ".runtime.xml";
    private static final Logger LOGGER = Logger.getLogger(GitHubPRRepository.class.getName());
    private transient XmlFile configFile; // for save()
    private transient AbstractProject<?, ?> project;  // for UI

    private final String fullName;
    private final String githubUrl;

    private HashMap<Integer, GitHubPRPullRequest> pulls;

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

    @RequirePOST
    public FormValidation doClearRepo() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubPRTrigger.DescriptorImpl.getJenkinsInstance();
            if (instance.hasPermission(Item.DELETE)) {
                pulls = null;
                save();
                result = FormValidation.ok("Pulls deleted");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can\'t delete repository file '{0}', '{1}'",
                    new Object[] {configFile.getFile().getAbsolutePath(), e.getMessage()});
            result = FormValidation.error("Can't delete: " + e.getMessage());
        }
        return result;
    }

    @RequirePOST
    public FormValidation doRebuildFailed() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubPRTrigger.DescriptorImpl.getJenkinsInstance();
            if (instance.hasPermission(Item.BUILD)) {
                Map<Integer, List<AbstractBuild<?, ?>>> builds = getAllPrBuilds();
                for (List<AbstractBuild<?, ?>> buildList : builds.values()) {
                    if (!buildList.isEmpty() && Result.FAILURE.equals(buildList.get(0).getResult())) {
                        AbstractBuild<?, ?> lastBuild = buildList.get(0);
                        rebuild(lastBuild);
                    }
                }
                result = FormValidation.ok("Rebuild scheduled");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't start rebuild", e.getMessage());
            result = FormValidation.error("Can't start rebuild: " + e.getMessage());
        }
        return result;
    }

    @RequirePOST
    public FormValidation doRebuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            Jenkins instance = GitHubPRTrigger.DescriptorImpl.getJenkinsInstance();
            if (!instance.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String prNumberParam = "prNumber";
            int prId = 0;
            if (req.hasParameter(prNumberParam)) {
                prId = Integer.valueOf(req.getParameter(prNumberParam));
            }

            Map<Integer, List<AbstractBuild<?, ?>>> builds = getAllPrBuilds();
            List<AbstractBuild<?, ?>> prBuilds = builds.get(prId);
            if (prBuilds != null && !prBuilds.isEmpty()) {
                if (rebuild(prBuilds.get(0))) {
                    result = FormValidation.ok("Rebuild scheduled");
                } else {
                    result = FormValidation.warning("Rebuild not scheduled");
                }
            } else {
                result = FormValidation.warning("Build not found");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Can't start rebuild", e.getMessage());
            result = FormValidation.error("Can't start rebuild: " + e.getMessage());
        }
        return result;
    }

    private static boolean rebuild(AbstractBuild<?, ?> build) {
        final List<Action> actions = new ArrayList<Action>();
//
//        List<Cause> causeList = new ArrayList<>(build.getCauses());
//        causeList.add(new Cause.UserIdCause());
//
//        actions.add(build.getAction(ParametersAction.class));
//        actions.add(new CauseAction(causeList));

        actions.add(build.getAction(ParametersAction.class));
        actions.add(build.getAction(CauseAction.class));

        return build.getProject().scheduleBuild(0, new Cause.UserIdCause(), actions.toArray(new Action[actions.size()]));
    }

    public void setProject(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
    }
}
