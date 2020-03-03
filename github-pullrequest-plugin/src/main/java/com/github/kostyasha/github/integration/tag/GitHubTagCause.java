package com.github.kostyasha.github.integration.tag;

import com.github.kostyasha.github.integration.branch.AbstractGitHubBranchCause;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import hudson.model.ParameterValue;
import org.kohsuke.github.GHTag;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubTagCause extends AbstractGitHubBranchCause<GitHubTagCause> {

    private final String tagName;

    public GitHubTagCause(@Nonnull GitHubTag localTag, @Nonnull GitHubTagRepository localRepo, String reason, boolean skip) {
        this(localTag.getName(), localTag.getCommitSha());
        withReason(reason);
        withSkip(skip);
        withLocalRepo(localRepo);
    }

    public GitHubTagCause(@Nonnull GHTag remoteTag, @Nonnull GitHubTagRepository localRepo, String reason, boolean skip) {
        this(remoteTag.getName(), remoteTag.getCommit().getSHA1());
        withReason(reason);
        withSkip(skip);
        withLocalRepo(localRepo);
        withRemoteData(remoteTag);
    }

    public GitHubTagCause(@Nonnull String tagName, String commitSha) {
        super(commitSha, "refs/tags/" + tagName);
        this.tagName = tagName;
    }

    /**
     * Copy constructor
     */
    public GitHubTagCause(GitHubTagCause cause) {
        super(cause);
        this.tagName = cause.getTagName();
    }

    public String getTagName() {
        return tagName;
    }

    @Nonnull
    @Override
    public String getShortDescription() {
        if (getHtmlUrl() != null) {
            return "GitHub tag " + getTagName() + ": " + getReason();
        } else {
            return "Deleted tag";
        }
    }

    @Override
    public void fillParameters(List<ParameterValue> params) {
        GitHubTagEnv.getParams(this, params);
    }

    @Override
    public GitHubSCMHead<GitHubTagCause> createSCMHead(String sourceId) {
        return new GitHubTagSCMHead(tagName, sourceId);
    }
}
