package com.github.kostyasha.github.integration.tag;

import static com.github.kostyasha.github.integration.generic.GitHubTagDecisionContext.newGitHubTagDecisionContext;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.kohsuke.github.GHTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.generic.GitHubTagDecisionContext;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.handler.GitHubTagHandler;
import com.github.kostyasha.github.integration.tag.events.GitHubTagEvent;

import hudson.model.TaskListener;

/**
 * @author Kanstantsin Shautsou
 */
public class TagToCauseConverter implements Function<GHTag, GitHubTagCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagToCauseConverter.class);

    private final GitHubTagRepository localTags;
    private final TaskListener listener;
    private final GitHubTagHandler handler;
    private final GitHubSCMSource source;

    public TagToCauseConverter(@Nonnull GitHubTagRepository localTags,
                                  @Nonnull TaskListener listener,
                                  @Nonnull GitHubTagHandler handler,
                                  @Nonnull GitHubSCMSource source) {
        this.localTags = localTags;
        this.listener = listener;
        this.handler = handler;
        this.source = source;
    }

    private List<GitHubTagEvent> getEvents() {
        return handler.getEvents();
    }

    @Override
    public GitHubTagCause apply(final GHTag remoteTag) {
        List<GitHubTagCause> causes = getEvents().stream()
                .map(event -> toCause(event, remoteTag))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String name = remoteTag.getName();
        if (causes.isEmpty()) {
            LOGGER.debug("No build trigger causes found for tag [{}].", name);
            return null;
        }

        LOGGER.debug("All matched events for tag [{}] : {}.", name, causes);

        GitHubTagCause cause = GitHubCause.skipTrigger(causes);
        if (cause != null) {
            LOGGER.debug("Cause [{}] indicated build should be skipped.", cause);
            listener.getLogger().println(String.format("Build of tag %s skipped: %s.", name, cause.getReason()));
            return null;
        } else if (!causes.isEmpty()) {
            // use the first cause from the list
            cause = causes.get(0);
            LOGGER.debug("Using build cause [{}] as trigger for tag [{}].", cause, name);
        }

        return cause;
    }

    private GitHubTagCause toCause(GitHubTagEvent event, GHTag remoteTag) {
        String tagName = remoteTag.getName();
        GitHubTag localTag = localTags.getTags().get(tagName);

        try {

            GitHubTagDecisionContext context = newGitHubTagDecisionContext()
                    .withListener(listener)
                    .withLocalRepo(localTags)
                    .withRemoteTag(remoteTag)
                    .withLocalTag(localTag)
                    .withTagHandler(handler)
                    .withSCMSource(source)
                    .build();

            return event.check(context);
        } catch (IOException e) {
            LOGGER.error("Event check failed, skipping tag [{}].", tagName, e);
            listener.error("Event check failed, skipping tag [{}] {}", tagName, e);

            return null;
        }
    }

}
