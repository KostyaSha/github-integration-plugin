package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;
import com.google.common.base.Splitter;
import hudson.Extension;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubCommitMessageEvent extends GitHubBranchEvent {
    private static final String DISPLAY_NAME = "Commit Message Pattern";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchHashChangedEvent.class);

    /**
     * Max number of commits for checking. Critical in case of force-pushes
     * when we can't know the depth of search.
     */
    private int maxCommits = 1000;

    private boolean skip = false;

    private String matchPatternsStr = "";

    // performance
    private transient Set<String> matchPatterns;

    @DataBoundConstructor
    public GitHubCommitMessageEvent() {
    }

    // max commits
    public int getMaxCommits() {
        return maxCommits;
    }

    @DataBoundSetter
    public void setMaxCommits(int maxCommits) {
        this.maxCommits = maxCommits;
    }

    // skip
    public boolean isSkip() {
        return skip;
    }

    @DataBoundSetter
    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    // UI binding
    @Nonnull
    public String getMatchPatternsStr() {
        return trimToEmpty(matchPatternsStr);
    }

    @DataBoundSetter
    public void setMatchPatternsStr(String matchPatternsStr) {
        this.matchPatternsStr = matchPatternsStr;
    }

    @Nonnull
    public Set<String> getMatchPatterns() {
        if (isNull(matchPatterns)) {
            final HashSet<String> patterns = new HashSet<>();

            final Iterable<String> split = Splitter.on(System.lineSeparator())
                    .trimResults()
                    .omitEmptyStrings()
                    .split(getMatchPatternsStr());

            for (String s : split) {
                matchPatterns.add(s);
            }
        }

        return matchPatterns;
    }

    @Override
    public GitHubBranchCause check(GitHubBranchTrigger trigger,
                                   GHBranch remoteBranch,
                                   @CheckForNull GitHubBranch localBranch,
                                   GitHubBranchRepository localRepo,
                                   TaskListener listener) throws IOException {
        GitHubBranchCause cause = null;
        final String prev = localBranch.getCommitSha();
        final String current = remoteBranch.getSHA1();

        final GHCompare compare = remoteBranch.getOwner().getCompare(current, prev);
        final GHCompare.Commit[] commits = compare.getCommits();

        boolean matched = false;
        for (GHCompare.Commit commit : commits) {
            final String message = commit.getCommit().getMessage();
            for (String match : getMatchPatterns()) {
                //if branch name matches to pattern, allow build
                matched = Pattern.compile(match).matcher(message).matches();
                if (matched) {
                    return new GitHubBranchCause(remoteBranch, localRepo, "Message " + message + " matched", skip);
                }
            }
        }

        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubBranchEventDescriptor {
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
