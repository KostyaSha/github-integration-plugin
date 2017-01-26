package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.impl.commitchecks.GitHubBranchCommitCheck;
import com.github.kostyasha.github.integration.branch.events.impl.commitchecks.GitHubBranchCommitCheckDescriptor;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

import hudson.Extension;
import hudson.model.TaskListener;

import net.sf.json.JSONObject;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This branch event acts as a wrapper around checks that can be performed against commit data that requires
 * an additional round trip to GitHub to retrieve.
 * <p>
 * Commit data is retrieved and then passed to each implementing instance of <code>GitHubBranchCommitCheck</code>
 * to determine information about the commit should trigger a build.
 * </p>
 *
 * @author Kanstantsin Shautsou
 * @author Jae Gangemi
 */
public class GitHubBranchCommitEvent extends GitHubBranchEvent {
    private static final String DISPLAY_NAME = "Commit Checks";

    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchCommitEvent.class);

    private List<GitHubBranchCommitCheck> checks = new ArrayList<>();

    /**
     * For groovy UI
     */
    @Restricted(value = NoExternalUse.class)
    public GitHubBranchCommitEvent() {
    }

    @DataBoundConstructor
    public GitHubBranchCommitEvent(List<GitHubBranchCommitCheck> checks) {
        this.checks = checks;
    }

    @Override
    public GitHubBranchCause check(GitHubBranchTrigger trigger, GHBranch remoteBranch, @CheckForNull GitHubBranch localBranch,
                                   GitHubBranchRepository localRepo, TaskListener listener)
            throws IOException {
        final PrintStream lloger = listener.getLogger();

        if (localBranch == null) {
            LOG.debug("First build of branch '%s' detected, skipping check.", remoteBranch.getName());
            lloger.println("First build of branch '" + remoteBranch.getName() + "' detected, skipping check.");
            return null;
        }

        GHCompare.Commit[] commits = getComparedCommits(localBranch, remoteBranch);
        List<GitHubBranchCause> causes = checks.stream()
                .map(event -> event.check(remoteBranch, localRepo, commits))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String name = remoteBranch.getName();
        if (causes.isEmpty()) {
            LOG.debug("Commits for branch [{}] had no effect, not triggering run.", name);
            return null;
        }

        GitHubBranchCause cause = causes.get(0);
        LOG.info("Building branch [{}] skipped due to commit check: {}", name, cause.getReason());

        return cause;
    }

    // visible for testing to avoid complex mocking
    GHCompare.Commit[] getComparedCommits(GitHubBranch localBranch, GHBranch remoteBranch) throws IOException {
        String previous = localBranch.getCommitSha();
        String current = remoteBranch.getSHA1();

        LOG.debug("Comparing previous hash [{}] with current hash [{}]", previous, current);
        return remoteBranch.getOwner()
                .getCompare(previous, current)
                .getCommits();
    }

    @Nonnull
    public List<GitHubBranchCommitCheck> getChecks() {
        if (isNull(checks)) {
            checks = new ArrayList<>();
        }
        return checks;
    }

    public void setChecks(List<GitHubBranchCommitCheck> checks) {
        this.checks = checks;
    }

    @Extension
    public static class DescriptorImpl extends GitHubBranchEventDescriptor {
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);

            save();
            return super.configure(req, formData);
        }

        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }

        public List<GitHubBranchCommitCheckDescriptor> getEventDescriptors() {
            return GitHubBranchCommitCheckDescriptor.all();
        }
    }
}
