package com.github.kostyasha.github.integration.multibranch.handler;

import static com.github.kostyasha.github.integration.branch.trigger.check.LocalRepoUpdater.updateLocalRepo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.trigger.check.BranchToCauseConverter;
import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.hooks.GitHubBranchSCMHeadEvent;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;

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

    @Override
    public void handle(@Nonnull GitHubSourceContext context) throws IOException {
        
        String branchName;

        SCMHeadEvent<?> scmHeadEvent = context.getScmHeadEvent();
        if (scmHeadEvent instanceof GitHubBranchSCMHeadEvent) {
            BranchInfo info = (BranchInfo) scmHeadEvent.getPayload();
            branchName = info.getBranchName();
        } else if (scmHeadEvent != null) {
            // not our event, skip completely
            return;
        } else {
            branchName = null;
        }

        GitHubSCMSource source = context.getSource();
        GitHubRepo localRepo = context.getLocalRepo();
        GitHubBranchRepository branchRepository = localRepo.getBranchRepository();
        GitHub github = context.getGitHub();
        TaskListener listener = context.getListener();

        if (branchName != null) {
            listener.getLogger().println("**** Processing branch " + branchName + " ****");
        } else {
            listener.getLogger().println("**** Processing branches ****");
        }

        GHRateLimit rateLimitBefore = github.getRateLimit();
        listener.getLogger().println("GitHub rate limit before check: " + rateLimitBefore);

        // get local and remote list of branches
        Set<GHBranch> remoteBranches = branchesToCheck(branchName, context);
        
        Objects.requireNonNull(branchRepository);

        // triggering logic and result
        List<GitHubBranchCause> causes = checkBranches(remoteBranches, branchRepository, source, listener);

        if (branchName != null) {
            branchRepository.getBranches().remove(branchName);
        } else {
            branchRepository.getBranches().clear();
        }
        remoteBranches.stream().map(updateLocalRepo(branchRepository)).count();

        GHRateLimit rateLimitAfter = github.getRateLimit();
        int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
        LOG.info("GitHub rate limit after check {}: {}, consumed: {}, checked branches: {}",
                source.getRepoFullName(), rateLimitAfter, consumed, remoteBranches.size());
        HashSet<String> triggeredBranches = new HashSet<>();

        // trigger builds
        causes.forEach(branchCause -> {
            String commitSha = branchCause.getCommitSha();
            GitHubBranchSCMHead scmHead = new GitHubBranchSCMHead(branchCause.getBranchName(), source.getId());
            GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, commitSha, branchCause);
            try {
                context.forceNextBuild(scmRevision);
                context.getObserver().observe(scmHead, scmRevision);
                triggeredBranches.add(scmHead.getName());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.getLogger());
            }
        });

        // notify untriggered items that we're still interested in them
        branchRepository.getBranches().entrySet().stream()
                // only handle the requested branch if present
                .filter(it -> branchName == null || branchName.equals(it.getKey()))
                // and only if it wasn't triggered
                .filter(it -> !triggeredBranches.contains(it.getKey()))
                .map(Map.Entry::getValue)
                .forEach(value -> {
                    try {
                        GitHubBranchSCMHead scmHead = new GitHubBranchSCMHead(value.getName(), source.getId());
                        GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, value.getCommitSha(), null);
                        context.getObserver().observe(scmHead, scmRevision);
                    } catch (IOException | InterruptedException e) {
                        // try as much as can
                        e.printStackTrace(listener.getLogger());
                    }
                });

        listener.getLogger().println("**** Done processing branches ****\n");
    }

    /**
     * Remote branch for future analysing. null - all remote branches.
     */
    private Set<GHBranch> branchesToCheck(@CheckForNull String branch, @Nonnull GitHubSourceContext context) throws IOException {
        return filterOutUninteresting(fetchRemoteBranches(branch, context.getRemoteRepo()), context);
    }

    private static Set<GHBranch> fetchRemoteBranches(@CheckForNull String branch, GHRepository remoteRepo) throws IOException {
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

    private static Set<GHBranch> filterOutUninteresting(Set<GHBranch> branches, @Nonnull GitHubSourceContext context) throws IOException {
        Set<GHBranch> newBranches = new HashSet<>();
        for (GHBranch branch : branches) {
            if (isInteresting(branch, context)) {
                newBranches.add(branch);
            }
        }
        return newBranches;
    }

    private static boolean isInteresting(@Nonnull GHBranch ghBranch, @Nonnull GitHubSourceContext context) throws IOException {
        GitHubBranchSCMHead head = new GitHubBranchSCMHead(ghBranch.getName(), context.getSource().getId());
        GitHubSCMRevision revision = new GitHubSCMRevision(head, ghBranch.getSHA1(), null);
        return context.checkCriteria(head, revision);
    }

    private List<GitHubBranchCause> checkBranches(Set<GHBranch> remoteBranches,
                                                  @Nonnull GitHubBranchRepository localBranches,
                                                  @Nonnull GitHubSCMSource source,
                                                  @Nonnull TaskListener listener) {
        List<GitHubBranchCause> causes = remoteBranches.stream()
                // TODO: update user whitelist filter
                .filter(Objects::nonNull)
                .map(new BranchToCauseConverter(localBranches, listener, this, source))
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
