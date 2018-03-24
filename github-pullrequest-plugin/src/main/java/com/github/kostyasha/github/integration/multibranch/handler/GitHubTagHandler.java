package com.github.kostyasha.github.integration.multibranch.handler;

import static com.github.kostyasha.github.integration.tag.LocalRepoUpdater.updateLocalRepo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kostyasha.github.integration.branch.webhook.BranchInfo;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import com.github.kostyasha.github.integration.multibranch.hooks.GitHubTagSCMHeadEvent;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
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
    private static final Logger LOG = LoggerFactory.getLogger(GitHubTagHandler.class);

    private List<GitHubTagEvent> events = new ArrayList<>();

    @DataBoundConstructor
    public GitHubTagHandler() {
    }

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

        GitHubSCMSource source = context.getSource();
        GitHubRepo localRepo = context.getLocalRepo();
        GitHubTagRepository tagRepository = localRepo.getTagRepository();
        GitHub github = context.getGitHub();
        TaskListener listener = context.getListener();

        if (tagName != null) {
            listener.getLogger().println("**** Processing tag " + tagName + " ****");
        } else {
            listener.getLogger().println("**** Processing tags ****");
        }

        GHRateLimit rateLimitBefore = github.getRateLimit();
        listener.getLogger().println("GitHub rate limit before check: " + rateLimitBefore);

        // get local and remote list of tags
        Set<GHTag> remoteTags = tagsToCheck(tagName, context);
        
        Objects.requireNonNull(tagRepository);

        // triggering logic and result
        List<GitHubTagCause> causes = checkTags(remoteTags, tagRepository, source, listener);

        if (tagName != null) {
            tagRepository.getTags().remove(tagName);
        } else {
            tagRepository.getTags().clear();
        }
        remoteTags.stream().map(updateLocalRepo(tagRepository)).count();

        GHRateLimit rateLimitAfter = github.getRateLimit();
        int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
        LOG.info("GitHub rate limit after check {}: {}, consumed: {}, checked tagss: {}",
                source.getRepoFullName(), rateLimitAfter, consumed, remoteTags.size());
        HashSet<String> triggeredTags = new HashSet<>();

        // trigger builds
        causes.forEach(tagCause -> {
            String commitSha = tagCause.getCommitSha();
            GitHubTagSCMHead scmHead = new GitHubTagSCMHead(tagCause.getTagName(), source.getId());
            GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, commitSha, tagCause);
            try {
                context.forceNextBuild(scmRevision);
                context.getObserver().observe(scmHead, scmRevision);
                triggeredTags.add(scmHead.getName());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace(listener.getLogger());
            }
        });

        // notify untriggered items that we're still interested in them
        tagRepository.getTags().entrySet().stream()
                // only handle the requested tag if present
                .filter(it -> tagName == null || tagName.equals(it.getKey()))
                // and only if it wasn't triggered
                .filter(it -> !triggeredTags.contains(it.getKey()))
                .map(Map.Entry::getValue)
                .forEach(value -> {
                    try {
                        GitHubTagSCMHead scmHead = new GitHubTagSCMHead(value.getName(), source.getId());
                        GitHubSCMRevision scmRevision = new GitHubSCMRevision(scmHead, value.getCommitSha(), null);
                        context.getObserver().observe(scmHead, scmRevision);
                    } catch (IOException | InterruptedException e) {
                        // try as much as can
                        e.printStackTrace(listener.getLogger());
                    }
                });

        listener.getLogger().println("**** Done processing tags ****\n");
    }

    /**
     * Remote tag for future analysing. null - all remote tags.
     */
    private Set<GHTag> tagsToCheck(@CheckForNull String tag, @Nonnull GitHubSourceContext context) throws IOException {
        return filterOutUninteresting(fetchRemoteTag(tag, context.getRemoteRepo()), context);
    }

    private static Set<GHTag> fetchRemoteTag(@CheckForNull String tag, GHRepository remoteRepo) throws IOException {

        if (tag != null) {
            GHTag ghTag = GitHubTag.findRemoteTag(remoteRepo, tag);
            if (ghTag != null) {
                return Collections.singleton(ghTag);
            }
            return Collections.emptySet();
        }

        return new LinkedHashSet<>(GitHubTag.getAllTags(remoteRepo));
    }

    private static Set<GHTag> filterOutUninteresting(Set<GHTag> tags, @Nonnull GitHubSourceContext context) throws IOException {
        Set<GHTag> newTags = new HashSet<>();
        for (GHTag tag : tags) {
            if (isInteresting(tag, context)) {
                newTags.add(tag);
            }
        }
        return newTags;
    }

    private static boolean isInteresting(@Nonnull GHTag ghTag, @Nonnull GitHubSourceContext context) throws IOException {
        GitHubTagSCMHead head = new GitHubTagSCMHead(ghTag.getName(), context.getSource().getId());
        GitHubSCMRevision revision = new GitHubSCMRevision(head, ghTag.getCommit().getSHA1(), null).setRemoteData(ghTag);
        return context.checkCriteria(head, revision);
    }

    private List<GitHubTagCause> checkTags(Set<GHTag> remoteTags,
                                                  @Nonnull GitHubTagRepository localTags,
                                                  @Nonnull GitHubSCMSource source,
                                                  @Nonnull TaskListener listener) {
        List<GitHubTagCause> causes = remoteTags.stream()
                // TODO: update user whitelist filter
                .filter(Objects::nonNull)
                .map(new TagToCauseConverter(localTags, listener, this, source))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        LOG.debug("Build trigger count for [{}] : {}", localTags.getFullName(), causes.size());
        return causes;
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
