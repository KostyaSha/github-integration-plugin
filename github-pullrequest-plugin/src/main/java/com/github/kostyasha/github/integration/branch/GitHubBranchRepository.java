package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.branch.trigger.JobRunnerForBranchCause;
import com.github.kostyasha.github.integration.generic.GitHubRepository;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.util.FormValidation;
import hudson.util.RunList;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchCauseFromRun;
import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.rebuild;

/**
 * Store local state of remote branches.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchRepository extends GitHubRepository<GitHubBranchRepository> {
    /**
     * Store constantly changing information in job directory with .runtime.xml tail
     */
    public static final String FILE = GitHubBranchRepository.class.getName() + ".runtime.xml";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchRepository.class);

    private Map<String, GitHubBranch> branches = new ConcurrentHashMap<>();

    /**
     * Object that represent GitHub repository to work with
     *
     * @param remoteRepository remote repository full name.
     */
    public GitHubBranchRepository(GHRepository remoteRepository) throws IOException {
        super(remoteRepository);
    }

    public GitHubBranchRepository(String repoFullName, URL url) {
        super(repoFullName, url);
    }

    @NonNull
    public Map<String, GitHubBranch> getBranches() {
        if (isNull(branches)) branches = new ConcurrentHashMap<>();
        return branches;
    }

    @Override
    public void actualiseOnChange(@NonNull GHRepository ghRepository, @NonNull TaskListener listener) {
        if (changed) {
            listener.getLogger().println("Local settings changed, removing branches in repository!");
            getBranches().clear();
        }
    }

    @Override
    public String getIconFileName() {
        return GitHubBranch.getIconFileName();
    }

    @Override
    public String getDisplayName() {
        return "GitHub Branches";
    }

    @Override
    public String getUrlName() {
        return "github-branch";
    }


    /**
     * Searches for all builds performed in the runs of current job.
     *
     * @return map with key - string branch names; value - lists of related builds.
     */
    public Map<String, List<Run<?, ?>>> getAllBranchBuilds() {

        Map<String, List<Run<?, ?>>> map = new HashMap<>();
        final RunList<?> runs = job.getBuilds();
        LOG.debug("Got builds for job {}", job.getFullName());

        for (Run<?, ?> run : runs) {
            GitHubBranchCause cause = ghBranchCauseFromRun(run);
            if (cause != null) {
                String branchName = cause.getBranchName();
                List<Run<?, ?>> buildsByBranchName = map.get(branchName);
                if (isNull(buildsByBranchName)) {
                    buildsByBranchName = new ArrayList<>();
                    map.put(branchName, buildsByBranchName);
                }
                buildsByBranchName.add(run);
            }
        }

        return map;
    }

    @Override
    @RequirePOST
    public FormValidation doClearRepo() throws IOException {
        LOG.debug("Got clear GitHub Branch repo request for {}", getJob().getFullName());
        FormValidation result;
        try {
            if (job.hasPermission(Item.DELETE)) {
                branches.clear();
                save();
                result = FormValidation.ok("Branches deleted");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can't delete repository file '{}'.",
                    configFile.getFile().getAbsolutePath(), e);
            result = FormValidation.error(e, "Can't delete: " + e.getMessage());
        }
        return result;
    }

    @Override
    @RequirePOST
    public FormValidation doRunTrigger() throws IOException {
        FormValidation result;
        try {
            if (job.hasPermission(Item.BUILD)) {
                GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
                if (trigger != null) {
                    trigger.run();
                    result = FormValidation.ok("GitHub Branch trigger run");
                    LOG.debug("GitHub Branch trigger run for {}", job);
                } else {
                    LOG.error("GitHub Branch trigger not available for {}", job);
                    result = FormValidation.error("GitHub Branch trigger not available");
                }
            } else {
                LOG.warn("No permissions to run GitHub Branch trigger");
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can't run trigger", e.getMessage());
            result = FormValidation.error(e, "Can't run trigger: %s", e.getMessage());
        }
        return result;
    }

    @Override
    @RequirePOST
    public FormValidation doRebuildAllFailed() throws IOException {
        FormValidation result;
        try {
            if (job.hasPermission(Item.BUILD)) {
                Map<String, List<Run<?, ?>>> builds = getAllBranchBuilds();
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
            LOG.error("Can't start rebuild", e.getMessage());
            result = FormValidation.error(e, "Can't start rebuild: %s", e.getMessage());
        }
        return result;
    }

    @RequirePOST
    public FormValidation doBuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            if (!job.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String param = "branchName";
            String branchName = null;
            if (req.hasParameter(param)) {
                branchName = req.getParameter(param);
            }
            if (isNull(branchName) || !getBranches().containsKey(branchName)) {
                return FormValidation.error("No branch to build");
            }

            final GitHubBranch localBranch = getBranches().get(branchName);
            final GitHubBranchCause cause = new GitHubBranchCause(localBranch, this, "Manual run.", false);
            final JobRunnerForBranchCause runner = new JobRunnerForBranchCause(getJob(),
                    ghBranchTriggerFromJob(job));
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

    @Override
    @RequirePOST
    public FormValidation doRebuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            if (!job.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String param = "branchName";
            String branchName = "";
            if (req.hasParameter(param)) {
                branchName = req.getParameter(param);
            }

            Map<String, List<Run<?, ?>>> allBuilds = getAllBranchBuilds();
            List<Run<?, ?>> branchBuilds = allBuilds.get(branchName);
            if (branchBuilds != null && !allBuilds.isEmpty()) {
                if (rebuild(branchBuilds.get(0))) {
                    result = FormValidation.ok("Rebuild scheduled");
                } else {
                    result = FormValidation.warning("Rebuild not scheduled");
                }
            } else {
                result = FormValidation.warning("Build not found");
            }
        } catch (Exception e) {
            LOG.error("Can't start rebuild", e.getMessage());
            result = FormValidation.error(e, "Can't start rebuild: " + e.getMessage());
        }
        return result;
    }

}
