package com.github.kostyasha.github.integration.branch.webhook;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;

import java.io.IOException;
import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.lang.String.format;
import static net.sf.json.JSONObject.fromObject;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractGHBranchSubscriber extends GHEventsSubscriber {
    public static final Set<GHEvent> EVENTS = immutableEnumSet(GHEvent.PUSH, GHEvent.CREATE, GHEvent.DELETE);

    @Override
    protected Set<GHEvent> events() {
        return EVENTS;
    }

    protected BranchInfo extractRefInfo(GHEvent event, String payload) throws IOException {
        JSONObject json = fromObject(payload);
        if (EVENTS.contains(event)) {
            return fromJson(json);
        } else {
            throw new IllegalStateException(format("Did you add event %s in events() method?", event));
        }
    }

    protected BranchInfo fromJson(JSONObject json) {
        final String repo = json.getJSONObject("repository").getString("full_name");
        String branchName = json.getString("ref");
        String fullRef = branchName;

        if (branchName.startsWith("refs/heads/")) {
            branchName = branchName.replace("refs/heads/", "");
        }

        return new BranchInfo(repo, branchName, fullRef);
    }
}
