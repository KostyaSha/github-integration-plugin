package org.jenkinsci.plugins.github.pullrequest.webhook;

import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.lang.String.format;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractGHPullRequestSubsriber extends GHEventsSubscriber {

    @Override
    public Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PULL_REQUEST, GHEvent.ISSUE_COMMENT);
    }

    protected PullRequestInfo extractPullRequestInfo(GHEvent event, String payload, GitHub gh) throws IOException {
        switch (event) {
            case ISSUE_COMMENT: {
                GHEventPayload.IssueComment commentPayload = gh.parseEventPayload(new StringReader(payload), GHEventPayload.IssueComment.class);

                int prNumber = commentPayload.getIssue().getNumber();

                return new PullRequestInfo(commentPayload.getRepository().getFullName(), prNumber);
            }

            case PULL_REQUEST: {
                GHEventPayload.PullRequest pr = gh.parseEventPayload(new StringReader(payload), GHEventPayload.PullRequest.class);

                return new PullRequestInfo(pr.getPullRequest().getRepository().getFullName(), pr.getNumber());
            }

            default:
                throw new IllegalStateException(format("Did you add event %s in events() method?", event));
        }
    }
}
