package org.jenkinsci.plugins.github.pullrequest.restrictions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * Restriction by target branch (one or many).
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRBranchRestriction implements Describable<GitHubPRBranchRestriction> {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRBranchRestriction.class);

    private final Set<String> targetBranchList;

    @DataBoundConstructor
    public GitHubPRBranchRestriction(String targetBranch) {
        this(split(targetBranch));
    }

    public GitHubPRBranchRestriction(List<String> targetBranches) {
        this.targetBranchList = new HashSet<>(targetBranches);
        targetBranchList.remove("");
    }

    public boolean isBranchBuildAllowed(GHPullRequest remotePR) {
        String branchName = remotePR.getBase().getRef();
        //if allowed branch list is empty, it's allowed to build any branch
        boolean isAllowed = targetBranchList.isEmpty();

        for (String branch : targetBranchList) {
            //if branch name matches to pattern, allow build
            isAllowed = Pattern.compile(branch).matcher(branchName).matches();
            if (isAllowed) {
                break;
            }
        }

        LOGGER.trace("Target branch {} is {} in our whitelist of target branches", branchName,
                (isAllowed ? "" : "not "));
        return isAllowed;
    }

    public Set<String> getTargetBranchList() {
        return targetBranchList;
    }

    public String getTargetBranch() {
        // TODO check if System.lineSeparator() is correct separator
        return Joiner.on("\n").skipNulls().join(targetBranchList);
    }

    public Descriptor<GitHubPRBranchRestriction> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRBranchRestriction.class);
    }

    private static List<String> split(String targetBranch) {
        return Arrays.asList(targetBranch.trim().split(LINE_SEPARATOR));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRBranchRestriction> {
        @Override
        public String getDisplayName() {
            return "Target branch";
        }
    }
}
