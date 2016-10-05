package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import hudson.model.Run;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.writeStringToFile;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCause extends GitHubCause<GitHubBranchCause> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchCause.class);

    private final String branchName;

    /**
     * null for deleted branch.
     */
    @CheckForNull
    private final String commitSha;

    @CheckForNull
    private final String fullRef;

    public GitHubBranchCause(@Nonnull GitHubBranch localBranch, @Nonnull GitHubBranchRepository localRepo,
                             String reason, boolean skip) {
        withReason(reason);
        withSkip(skip);
        withLocalRepo(localRepo);
        this.branchName = localBranch.getName();
        this.commitSha = localBranch.getCommitSha();
        this.fullRef = "refs/heads/" + branchName;
    }

    public GitHubBranchCause(@Nonnull GHBranch remoteBranch,
                             @Nonnull GitHubBranchRepository localRepo,
                             String reason, boolean skip) {
        withReason(reason);
        withSkip(skip);
        withLocalRepo(localRepo);
        this.branchName = remoteBranch.getName();
        this.commitSha = remoteBranch.getSHA1();
        this.fullRef = "refs/heads/" + branchName;
    }

    public String getBranchName() {
        return branchName;
    }

    @CheckForNull
    public String getCommitSha() {
        return commitSha;
    }

    @CheckForNull
    public String getFullRef() {
        return fullRef;
    }

    @Nonnull
    @Override
    public String getShortDescription() {
        if (getHtmlUrl() != null) {
            return "GitHub Branch <a href=\"" + getHtmlUrl() + "\">" + branchName + "</a>, " + getReason();
        } else {
            return "Deleted branch";
        }
    }

    @Override
    public void onAddedTo(@Nonnull Run run) {
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
