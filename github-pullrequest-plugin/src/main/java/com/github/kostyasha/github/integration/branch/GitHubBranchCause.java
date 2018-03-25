package com.github.kostyasha.github.integration.branch;

import hudson.model.ParameterValue;
import hudson.model.Run;
import jenkins.scm.api.SCMSourceOwner;

import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

import static org.apache.commons.io.FileUtils.writeStringToFile;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCause extends AbstractGitHubBranchCause<GitHubBranchCause> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchCause.class);

    private final String branchName;

    public GitHubBranchCause(@Nonnull GitHubBranch localBranch, @Nonnull GitHubBranchRepository localRepo, String reason, boolean skip) {
        this(localBranch.getName(), localBranch.getCommitSha());
        withReason(reason);
        withSkip(skip);
        withLocalRepo(localRepo);
    }

    public GitHubBranchCause(@Nonnull GHBranch remoteBranch, @Nonnull GitHubBranchRepository localRepo, String reason, boolean skip) {
        this(remoteBranch.getName(), remoteBranch.getSHA1());
        withReason(reason);
        withSkip(skip);
        withLocalRepo(localRepo);
    }

    public GitHubBranchCause(@Nonnull String branchName, String commitSha) {
        super(commitSha, "refs/heads/" + branchName);
        this.branchName = branchName;
    }

    /**
     * Copy constructor
     */
    public GitHubBranchCause(GitHubBranchCause cause) {
        super(cause);
        this.branchName = cause.getBranchName();
    }

    public String getBranchName() {
        return branchName;
    }

    @Override
    public void fillParameters(List<ParameterValue> params) {
        GitHubBranchEnv.getParams(this, params);
    }

    @Nonnull
    @Override
    public String getShortDescription() {
        if (getHtmlUrl() != null) {
            return "GitHub Branch <a href=\"" + getHtmlUrl() + "\">" + getBranchName() + "</a>, " + getReason();
        } else {
            return "Deleted branch";
        }
    }

    @Override
    public void onAddedTo(@Nonnull Run run) {
        if (run.getParent().getParent() instanceof SCMSourceOwner) {
            // skip multibranch
            return;
        }

        // move polling log from cause to action
        try {
            GitHubBranchPollingLogAction action = new GitHubBranchPollingLogAction(run);
            writeStringToFile(action.getPollingLogFile(), getPollingLog());
            run.replaceAction(action);
        } catch (IOException ex) {
            LOG.warn("Failed to persist the polling log", ex);
        }
        setPollingLog(null);
    }

}
