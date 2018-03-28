package com.github.kostyasha.github.integration.multibranch.handler;

import static com.github.kostyasha.github.integration.tag.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.ioOptStream;
import static org.jenkinsci.plugins.github.pullrequest.utils.IOUtils.iop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.hooks.GitHubTagSCMHeadEvent;
import com.github.kostyasha.github.integration.tag.GitHubTag;
import com.github.kostyasha.github.integration.tag.GitHubTagCause;
import com.github.kostyasha.github.integration.tag.GitHubTagRepository;
import com.github.kostyasha.github.integration.tag.TagToCauseConverter;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEvent;

import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHeadEvent;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubTagHandler extends GitHubHandler {

    private List<GitHubTagEvent> events = new ArrayList<>();

    @DataBoundConstructor
    public GitHubTagHandler() {}

    public List<GitHubTagEvent> getEvents() {
        return events;
    }

    @DataBoundSetter
    public GitHubTagHandler setEvents(List<GitHubTagEvent> events) {
        this.events = events;
        return this;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public void handle(@Nonnull GitHubSourceContext context) throws IOException {

        String tagName;

        SCMHeadEvent<?> scmHeadEvent = context.getScmHeadEvent();
        if (scmHeadEvent instanceof GitHubTagSCMHeadEvent) {
            BranchInfo info = (BranchInfo) scmHeadEvent.getPayload();
            tagName = info.getBranchName();
        } else if (scmHeadEvent != null) {
            // not our event, skip completely
            return;
        } else {
            tagName = null;
        }

        TaskListener listener = context.getListener();
        GHRepository remoteRepo = context.getRemoteRepo();
        GitHubTagRepository localRepo = Objects.requireNonNull(context.getLocalRepo().getTagRepository());
        GitHubTagRepository oldRepo = new GitHubTagRepository(remoteRepo);
        oldRepo.getTags().putAll(localRepo.getTags());

        // prepare for run and fetch remote tags
        Stream<GHTag> tags;
        if (tagName != null) {
            listener.getLogger().println("**** Processing tag " + tagName + " ****");
            tags = ioOptStream(() -> GitHubTag.findRemoteTag(remoteRepo, tagName));
            localRepo.getTags().remove(tagName);
        } else {
            listener.getLogger().println("**** Processing all tags ****");
            tags = GitHubTag.getAllTags(remoteRepo).stream();
            localRepo.getTags().clear();
        }

        processCauses(context, tags
                // filter out uninteresting
                .filter(iop(t -> context.checkCriteria(new GitHubTagCause(t, localRepo, "Check", false))))
                // update local state
                .map(updateLocalRepo(localRepo))
                // create causes
                .map(toCause(context, oldRepo)));

        listener.getLogger().println("**** Done processing tags ****");
    }

    private Function<GHTag, GitHubTagCause> toCause(GitHubSourceContext context, GitHubTagRepository localRepo) {
        TaskListener listener = context.getListener();
        GitHubSCMSource source = context.getSource();
        return t -> {
            GitHubTagCause c = new TagToCauseConverter(localRepo, listener, this, source).apply(t);
            if (c == null) {
                c = new GitHubTagCause(t, localRepo, "Skip", true);
            }
            return c;
        };
    }

    @Extension
    public static class DescriptorImpl extends GitHubHandlerDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Tag Handler";
        }
    }
}
