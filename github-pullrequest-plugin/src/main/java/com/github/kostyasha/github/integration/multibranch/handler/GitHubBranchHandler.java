package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.trigger.check.BranchToCauseConverter;
import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import hudson.Extension;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchHandler extends GitHubHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchHandler.class);

    private List<GitHubBranchEvent> events = new ArrayList<>();

    @DataBoundConstructor
    public GitHubBranchHandler() {
    }

    public List<GitHubBranchEvent> getEvents() {
        return events;
    }

    @DataBoundSetter
    public GitHubBranchHandler setEvents(List<GitHubBranchEvent> events) {
        this.events = events;
        return this;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Nonnull
    @Override
    public List<GitHubCause> handle(@Nonnull GitHubRepo localRepo, @Nonnull GHRepository remoteRepo,
                                    @Nonnull TaskListener listener, @Nonnull GitHubSCMSource source) {
        GitHubBranchRepository localBranches = localRepo.getBranchRepository();

        try {
            GitHub github = source.getRepoProvider().getGitHub(source);

            GHRateLimit rateLimitBefore = github.getRateLimit();
            listener.getLogger().println("GitHub rate limit before check: " + rateLimitBefore);

            // get local and remote list of branches
            Set<GHBranch> remoteBranches = branchesToCheck(null, remoteRepo, localBranches);

            Objects.requireNonNull(localBranches);

            List<GitHubCause> causes = checkBranches(remoteBranches, localBranches, listener);

            GHRateLimit rateLimitAfter = github.getRateLimit();
            int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
            LOG.info("GitHub rate limit after check {}: {}, consumed: {}, checked branches: {}",
                    source.getRepoFullName(), rateLimitAfter, consumed, remoteBranches.size());

            return causes;
        } catch (IOException e) {
            listener.error("Can't get build causes: '{}'", e);
        }

        return emptyList();
    }

    /**
     * Remote branch for future analysing. null - all remote branches.
     */
    private Set<GHBranch> branchesToCheck(@CheckForNull String branch, @Nonnull GHRepository remoteRepo,
                                          GitHubBranchRepository localRepository)
            throws IOException {
        final LinkedHashSet<GHBranch> ghBranches = new LinkedHashSet<>();

        if (branch != null) {
            final GHBranch ghBranch = remoteRepo.getBranches().get(branch);
            if (ghBranch != null) {
                ghBranches.add(ghBranch);
            }
        } else {
            ghBranches.addAll(remoteRepo.getBranches().values());
        }

        return ghBranches;
    }

    private List<GitHubCause> checkBranches(Set<GHBranch> remoteBranches,
                                            @Nonnull GitHubBranchRepository localBranches,
                                            @Nonnull TaskListener listener) {
        List<GitHubCause> causes = remoteBranches.stream()
                // TODO: update user whitelist filter
                .filter(Objects::nonNull)
                .map(new BranchToCauseConverter(localBranches, listener, this))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LOG.debug("Build trigger count for [{}] : {}", localBranches.getFullName(), causes.size());
        return causes;
    }

    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Branch Handler";
        }
    }
}
