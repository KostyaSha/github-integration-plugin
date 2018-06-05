package org.jenkinsci.plugins.github.pullrequest;

import com.github.kostyasha.github.integration.generic.GitHubRepository;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import hudson.util.FormValidation;
import hudson.util.RunList;
import org.jenkinsci.plugins.github.pullrequest.trigger.JobRunnerForCause;
import org.jenkinsci.plugins.github.pullrequest.utils.JobHelper;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRCauseFromRun;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.rebuild;

/**
 * GitHub Repository local state = last trigger run() state.
 * Store only necessary variables.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRRepository extends GitHubRepository<GitHubPRRepository> {
    /**
     * Store constantly changing information in job directory with .runtime.xml tail
     */
    public static final String FILE = GitHubPRRepository.class.getName() + ".runtime.xml";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubPRRepository.class);

    private final Map<Integer, GitHubPRPullRequest> pulls = new ConcurrentHashMap<>();

    /**
     * Object that represent GitHub repository to work with
     *
     * @param ghRepository remote repository.
     */
    public GitHubPRRepository(@Nonnull GHRepository ghRepository) throws IOException {
        super(ghRepository);
    }

    public GitHubPRRepository(String repoFullName, URL githubUrl) {
        super(repoFullName, githubUrl);
    }

    @Nonnull
    public Map<Integer, GitHubPRPullRequest> getPulls() {
        return pulls;
    }

    /**
     * Searches for all builds performed in the runs of current job.
     *
     * @return map with keys - numbers of built PRs and values - lists of related builds.
     */
    @Nonnull
    public Map<Integer, List<Run<?, ?>>> getAllPrBuilds() {

        Map<Integer, List<Run<?, ?>>> map = new HashMap<>();
        final RunList<?> runs = job.getBuilds();
        LOG.debug("Got builds for job {}", job.getFullName());

        for (Run<?, ?> run : runs) {
            GitHubPRCause cause = ghPRCauseFromRun(run);
            if (cause != null) {
                int number = cause.getNumber();
                List<Run<?, ?>> buildsByNumber = map.get(number);
                if (isNull(buildsByNumber)) {
                    buildsByNumber = new ArrayList<>();
                    map.put(number, buildsByNumber);
                }
                buildsByNumber.add(run);
            }
        }

        return map;
    }

    @Override
    public String getIconFileName() {
        return GitHubPRPullRequest.getIconFileName();
    }

    @Override
    public String getDisplayName() {
        return "GitHub PR";
    }

    @Override
    public String getUrlName() {
        return "github-pullrequest";
    }

    @RequirePOST
    public FormValidation doClearRepo() throws IOException {
        FormValidation result;
        try {
            if (job.hasPermission(Item.DELETE)) {
                pulls.clear();
                save();
                result = FormValidation.ok("Pulls deleted");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can\'t delete repository file '{}'",
                    configFile.getFile().getAbsolutePath(), e);
            result = FormValidation.error(e, "Can't delete: %s", e.getMessage());
        }
        return result;
    }

    /**
     * Run trigger from web.
     */
    @RequirePOST
    public FormValidation doRunTrigger() {
        FormValidation result;
        try {
            if (job.hasPermission(Item.BUILD)) {
                GitHubPRTrigger trigger = JobHelper.ghPRTriggerFromJob(job);
                if (trigger != null) {
                    trigger.run();
                    result = FormValidation.ok("GitHub PR trigger run");
                    LOG.debug("GitHub PR trigger run for {}", job);
                } else {
                    LOG.error("GitHub PR trigger not available for {}", job);
                    result = FormValidation.error("GitHub PR trigger not available");
                }
            } else {
                LOG.warn("No permissions to run GitHub PR trigger");
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can't run trigger", e);
            result = FormValidation.error(e, "Can't run trigger: %s", e.getMessage());
        }
        return result;
    }

    @RequirePOST
    public FormValidation doRebuildAllFailed() throws IOException {
        FormValidation result;
        try {
            if (job.hasPermission(Item.BUILD)) {
                Map<Integer, List<Run<?, ?>>> builds = getAllPrBuilds();
                for (List<Run<?, ?>> buildList : builds.values()) {
                    if (!buildList.isEmpty() && Result.FAILURE.equals(buildList.get(0).getResult())) {
                        Run<?, ?> lastBuild = buildList.get(0);
                        rebuild(lastBuild);
                    }
                }
                result = FormValidation.ok("Rebuild scheduled");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can't start rebuild", e);
            result = FormValidation.error(e, "Can't start rebuild: %s", e.getMessage());
        }
        return result;
    }

    @Override
    public FormValidation doBuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            if (!job.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String prNumberParam = "prNumber";
            int prId = 0;
            if (req.hasParameter(prNumberParam)) {
                prId = Integer.valueOf(req.getParameter(prNumberParam));
            }

            if (prId == 0 || !getPulls().containsKey(prId)) {
                return FormValidation.error("No branch to build");
            }

            final GitHubPRPullRequest localPR = getPulls().get(prId);
            final GitHubPRCause cause = new GitHubPRCause(localPR, null, this, false, "Manual run.");

            final JobRunnerForCause runner = new JobRunnerForCause(getJob(), ghPRTriggerFromJob(getJob()));
            final QueueTaskFuture<?> queueTaskFuture = runner.startJob(cause);

            if (nonNull(queueTaskFuture)) {
                result = FormValidation.ok("Build scheduled");
            } else {
                result = FormValidation.warning("Build not scheduled");
            }
        } catch (Exception e) {
            LOG.error("Can't start build", e.getMessage());
            result = FormValidation.error(e, "Can't start build: " + e.getMessage());
        }

        return result;
    }

    @RequirePOST
    public FormValidation doRebuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            if (!job.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String prNumberParam = "prNumber";
            int prId = 0;
            if (req.hasParameter(prNumberParam)) {
                prId = Integer.valueOf(req.getParameter(prNumberParam));
            }

            Map<Integer, List<Run<?, ?>>> builds = getAllPrBuilds();
            List<Run<?, ?>> prBuilds = builds.get(prId);
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
            LOG.error("Can't start rebuild", e);
            result = FormValidation.error(e, "Can't start rebuild: " + e.getMessage());
        }
        return result;
    }

    public void setJob(Job<?, ?> job) {
        this.job = job;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
    }
}
