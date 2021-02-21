package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.hooks.GitHubBranchSCMHeadEvent;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;
import org.jenkinsci.Symbol;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.kostyasha.github.integration.branch.trigger.check.BranchToCauseConverter.toGitHubBranchCause;
import static com.github.kostyasha.github.integration.branch.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.ioOptStream;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.iop;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchHandler extends GitHubHandler {
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
    public void handle(@NonNull GitHubSourceContext context) throws IOException {

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

        TaskListener listener = context.getListener();
        GHRepository remoteRepo = context.getRemoteRepo();
        GitHubBranchRepository localRepo = Objects.requireNonNull(context.getLocalRepo().getBranchRepository());
        GitHubBranchRepository oldRepo = new GitHubBranchRepository(remoteRepo);
        oldRepo.getBranches().putAll(localRepo.getBranches());

        // prepare for run and fetch remote branches
        Stream<GHBranch> branches;
        if (branchName != null) {
            listener.getLogger().println("**** Processing branch " + branchName + " ****");
            branches = ioOptStream(() -> remoteRepo.getBranch(branchName));
            localRepo.getBranches().remove(branchName);
        } else {
            listener.getLogger().println("**** Processing all branches ****");
            branches = fetchRemoteBranches(remoteRepo).values().stream();
            localRepo.getBranches().clear();
        }

        processCauses(context, branches
                // filter out uninteresting
                .filter(iop(b -> context.checkCriteria(new GitHubBranchCause(b, localRepo, "Check", false))))
                // update local state
                .map(updateLocalRepo(localRepo))
                // create causes
                .map(toCause(context, oldRepo)));

        listener.getLogger().println("**** Done processing branches ****");
    }

    private Function<GHBranch, GitHubBranchCause> toCause(GitHubSourceContext context, GitHubBranchRepository localRepo) {
        TaskListener listener = context.getListener();
        GitHubSCMSource source = context.getSource();
        return b -> {
            GitHubBranchCause c = toGitHubBranchCause(localRepo, listener, this, source).apply(b);
            if (c == null) {
                c = new GitHubBranchCause(b, localRepo, "Skip", true);
            }
            return c;
        };
    }

    private Map<String, GHBranch> fetchRemoteBranches(GHRepository remoteRepo) throws IOException {
        Map<String, GHBranch> branches = remoteRepo.getBranches();

        String defName = remoteRepo.getDefaultBranch();
        GHBranch def = branches.get(defName);
        if (def == null) { // just in case
            return branches;
        }

        // reorder branches so that default branch always comes first
        Map<String, GHBranch> ordered = new LinkedHashMap<>();
        ordered.put(defName, def);
        branches.entrySet().forEach(e -> {
            if (!e.getKey().equals(defName)) {
                ordered.put(e.getKey(), e.getValue());
            }
        });
        return ordered;
    }

    @Symbol("branches"/**, "branch"}**/)
    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Branch Handler";
        }
    }
}
